package com.shopguide.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopguide.entity.Cart;
import com.shopguide.entity.Category;
import com.shopguide.entity.Product;
import com.shopguide.mapper.CartMapper;
import com.shopguide.mapper.CategoryMapper;
import com.shopguide.mapper.ProductMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 助手可调用的业务工具（Function Calling）
 *
 * 每个 @Tool 方法都会被 LangChain4j 转成工具描述交给大模型，
 * 模型根据用户问题自行决定调用哪个方法、传什么参数，方法内部直接操作数据库。
 *
 * 注意：工具对象在每次建立 WebSocket 会话时按「当前登录用户」创建，
 * 所有购物车操作都使用构造时传入的 userId，不依赖 ThreadLocal，
 * 因为流式回调可能运行在模型 SDK 的线程上。
 */
public class ShopTools {

    private final Integer userId;
    private final String username;
    private final ProductMapper productMapper;
    private final CartMapper cartMapper;
    private final CategoryMapper categoryMapper;

    public ShopTools(Integer userId, String username,
                     ProductMapper productMapper, CartMapper cartMapper, CategoryMapper categoryMapper) {
        this.userId = userId;
        this.username = username;
        this.productMapper = productMapper;
        this.cartMapper = cartMapper;
        this.categoryMapper = categoryMapper;
    }

    /* ==================== 查询类 ==================== */

    @Tool("""
            按价格区间和商品分类查询在售商品，返回商品名称、价格、所属分类。
            用户让你推荐某价位、某分类的商品时必须先调用本工具，再根据返回结果回答，不能编造商品。
            """)
    public String searchProducts(
            @P("最低价格（元）") double minPrice,
            @P("最高价格（元）") double maxPrice,
            @P(value = "商品分类名称，例如“生活用品”“电子产品”；不限分类时传空字符串", required = false) String categoryName) {

        BigDecimal min = BigDecimal.valueOf(Math.max(0, minPrice));
        BigDecimal max = BigDecimal.valueOf(Math.max(0, maxPrice));
        if (min.compareTo(max) > 0) {
            BigDecimal tmp = min;
            min = max;
            max = tmp;
        }

        Integer categoryId = null;
        String cateLabel = "全部分类";
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            Category category = findCategory(categoryName.trim());
            if (category == null) {
                return "没有找到名为「" + categoryName.trim() + "」的分类。现有分类：" + categoryNames() + "。请据此重新查询。";
            }
            categoryId = category.getId();
            cateLabel = category.getName();
        }

        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getStatus, 1)
                .ge(Product::getPrice, min)
                .le(Product::getPrice, max)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .orderByAsc(Product::getPrice)
                .last("LIMIT 20");
        List<Product> list = productMapper.selectList(qw);

        if (list.isEmpty()) {
            return "没有找到价格在 ¥" + min + " ~ ¥" + max + "（" + cateLabel + "）的在售商品，可以建议用户放宽价格区间或换个分类。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(list.size()).append(" 件符合条件（价格 ¥").append(min).append(" ~ ¥").append(max)
                .append("，分类：").append(cateLabel).append("）的在售商品：\n");
        int i = 1;
        for (Product p : list) {
            sb.append(i++).append(". ").append(p.getName())
                    .append(" —— ¥").append(p.getPrice().setScale(2, java.math.RoundingMode.HALF_UP))
                    .append("（").append(categoryNameOf(p.getCategoryId())).append("）\n");
        }
        return sb.toString();
    }

    @Tool("查询系统现有的全部商品分类名称")
    public String listCategories() {
        return "当前商品分类：" + categoryNames();
    }

    @Tool("查看当前登录用户购物车中的商品（仅未支付）")
    public String viewCart() {
        List<Cart> list = unpaidCart();
        if (list.isEmpty()) {
            return "当前用户的购物车是空的。";
        }
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder sb = new StringBuilder("购物车共 " + list.size() + " 种商品：\n");
        for (Cart c : list) {
            BigDecimal subtotal = c.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
            total = total.add(subtotal);
            sb.append("- ").append(c.getProductName()).append(" ¥").append(c.getPrice())
                    .append(" × ").append(c.getQuantity()).append(" = ¥").append(subtotal.setScale(2, java.math.RoundingMode.HALF_UP)).append("\n");
        }
        sb.append("合计：¥").append(total.setScale(2, java.math.RoundingMode.HALF_UP));
        return sb.toString();
    }

    @Tool("查询当前登录用户的订单，statusText 传“未支付”或“已支付”，传空字符串表示全部")
    public String listOrders(@P("订单状态：“未支付”或“已支付”，传空字符串查全部") String statusText) {
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId);
        String label = "全部";
        if (statusText != null && statusText.contains("未")) {
            qw.eq(Cart::getStatus, Cart.STATUS_UNPAID);
            label = "未支付";
        } else if (statusText != null && statusText.contains("已")) {
            qw.eq(Cart::getStatus, Cart.STATUS_PAID);
            label = "已支付";
        }
        qw.orderByDesc(Cart::getCreateTime).last("LIMIT 20");
        List<Cart> list = cartMapper.selectList(qw);
        if (list.isEmpty()) {
            return "当前用户暂无" + label + "订单。";
        }
        StringBuilder sb = new StringBuilder("当前用户" + label + "订单共 " + list.size() + " 条：\n");
        for (Cart c : list) {
            sb.append("- ").append(c.getProductName()).append(" × ").append(c.getQuantity())
                    .append("，单价 ¥").append(c.getPrice())
                    .append("，状态：").append(c.getStatus() == Cart.STATUS_PAID ? "已支付" : "未支付")
                    .append("，下单时间：").append(c.getCreateTime()).append("\n");
        }
        return sb.toString();
    }

    /* ==================== 商品维护类 ==================== */

    @Tool("新增一个商品。需要商品名称、价格（元）和分类名称；用户明确要求新增商品时调用")
    public String addProduct(
            @P("商品名称") String name,
            @P("商品价格（元）") double price,
            @P(value = "商品分类名称，例如“生活用品”“电子产品”", required = false) String categoryName) {
        if (name == null || name.trim().isEmpty()) {
            return "新增失败：商品名称不能为空。";
        }
        if (price <= 0) {
            return "新增失败：商品价格必须大于 0。";
        }
        String cate = (categoryName == null || categoryName.trim().isEmpty()) ? "生活用品" : categoryName.trim();
        Category category = findCategory(cate);
        if (category == null) {
            return "新增失败：分类「" + cate + "」不存在。现有分类：" + categoryNames() + "。";
        }
        Product product = new Product();
        product.setName(name.trim());
        product.setPrice(BigDecimal.valueOf(price).setScale(2, java.math.RoundingMode.HALF_UP));
        product.setCategoryId(category.getId());
        product.setStatus(1);
        product.setImage("");
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        return "新增成功：商品「" + product.getName() + "」，价格 ¥" + product.getPrice() + "，分类「" + category.getName() + "」。";
    }

    @Tool("修改已有商品的信息（名称、价格、分类），按原商品名称定位；参数不修改时传空字符串或 0")
    public String updateProduct(
            @P("要修改的商品原名称（需准确）") String name,
            @P(value = "新的商品名称，不改则传空字符串", required = false) String newName,
            @P(value = "新的价格（元），不改则传 0", required = false) double newPrice,
            @P(value = "新的分类名称，不改则传空字符串", required = false) String newCategoryName) {
        Product product = byName(name);
        if (product == null) {
            return "修改失败：没有找到名为「" + name + "」的商品。可以先用查询工具确认商品名称。";
        }
        StringBuilder change = new StringBuilder();
        if (newName != null && !newName.trim().isEmpty()) {
            change.append("名称 ").append(product.getName()).append(" → ").append(newName.trim()).append("；");
            product.setName(newName.trim());
        }
        if (newPrice > 0) {
            BigDecimal price = BigDecimal.valueOf(newPrice).setScale(2, java.math.RoundingMode.HALF_UP);
            change.append("价格 ¥").append(product.getPrice()).append(" → ¥").append(price).append("；");
            product.setPrice(price);
        }
        if (newCategoryName != null && !newCategoryName.trim().isEmpty()) {
            Category category = findCategory(newCategoryName.trim());
            if (category == null) {
                return "修改失败：分类「" + newCategoryName.trim() + "」不存在。现有分类：" + categoryNames() + "。";
            }
            change.append("分类 → ").append(category.getName()).append("；");
            product.setCategoryId(category.getId());
        }
        if (change.length() == 0) {
            return "没有提供任何需要修改的内容，未做修改。";
        }
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        return "修改成功：商品「" + product.getName() + "」已更新（" + change + "）。";
    }

    @Tool("删除一个商品，按商品名称定位；删除后不可恢复，用户明确要求删除时调用")
    public String deleteProduct(@P("要删除的商品名称（需准确）") String name) {
        Product product = byName(name);
        if (product == null) {
            return "删除失败：没有找到名为「" + name + "」的商品。可以先用查询工具确认商品名称。";
        }
        productMapper.deleteById(product.getId());
        return "删除成功：商品「" + product.getName() + "」（¥" + product.getPrice() + "）已从商品表移除。";
    }

    /* ==================== 购物车操作类 ==================== */

    @Tool("把指定商品加入当前登录用户的购物车。需要商品名称和数量")
    public String addToCart(
            @P("商品名称（需准确）") String productName,
            @P(value = "购买数量，默认 1", required = false) int quantity) {
        Product product = byName(productName);
        if (product == null) {
            return "加入购物车失败：没有找到名为「" + productName + "」的商品。";
        }
        if (product.getStatus() != null && product.getStatus() == 0) {
            return "加入购物车失败：商品「" + product.getName() + "」已下架。";
        }
        int qty = quantity < 1 ? 1 : quantity;

        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, product.getId())
                .eq(Cart::getStatus, Cart.STATUS_UNPAID);
        Cart exist = cartMapper.selectOne(qw);
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + qty);
            exist.setUpdateTime(LocalDateTime.now());
            cartMapper.updateById(exist);
            return "已加入购物车：商品「" + product.getName() + "」数量累加为 " + exist.getQuantity() + " 件。";
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(product.getId());
        cart.setProductName(product.getName());
        cart.setPrice(product.getPrice());
        cart.setImage(product.getImage());
        cart.setQuantity(qty);
        cart.setStatus(Cart.STATUS_UNPAID);
        cart.setCreateTime(LocalDateTime.now());
        cart.setUpdateTime(LocalDateTime.now());
        cartMapper.insert(cart);
        return "已加入购物车：商品「" + product.getName() + "」× " + qty + "，小计 ¥"
                + product.getPrice().multiply(BigDecimal.valueOf(qty)).setScale(2, java.math.RoundingMode.HALF_UP) + "。";
    }

    @Tool("清空当前登录用户购物车中所有未支付的商品。用户说“清空购物车”时调用")
    public String clearCart() {
        List<Cart> list = unpaidCart();
        if (list.isEmpty()) {
            return "购物车本来就是空的，无需清空。";
        }
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId).eq(Cart::getStatus, Cart.STATUS_UNPAID);
        int count = cartMapper.delete(qw);
        return "已清空购物车：共移除 " + count + " 种商品（" + list.size() + " 条记录）。";
    }

    /* ==================== 内部工具方法 ==================== */

    private List<Cart> unpaidCart() {
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId)
                .eq(Cart::getStatus, Cart.STATUS_UNPAID)
                .orderByDesc(Cart::getCreateTime);
        return cartMapper.selectList(qw);
    }

    private Product byName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String key = name.trim();
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getName, key);
        Product exact = productMapper.selectOne(qw);
        if (exact != null) {
            return exact;
        }
        // 名称不完全一致时退化为模糊匹配，取第一条
        LambdaQueryWrapper<Product> like = new LambdaQueryWrapper<>();
        like.like(Product::getName, key).orderByAsc(Product::getId).last("LIMIT 1");
        return productMapper.selectOne(like);
    }

    private Category findCategory(String name) {
        LambdaQueryWrapper<Category> qw = new LambdaQueryWrapper<>();
        qw.eq(Category::getName, name).last("LIMIT 1");
        return categoryMapper.selectOne(qw);
    }

    private String categoryNames() {
        List<Category> list = categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        if (list.isEmpty()) {
            return "（暂无分类）";
        }
        return list.stream().map(Category::getName).collect(Collectors.joining("、"));
    }

    private String categoryNameOf(Integer categoryId) {
        if (categoryId == null) {
            return "未分类";
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? "未分类" : category.getName();
    }

    /** 当前工具绑定的用户（调试用） */
    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}

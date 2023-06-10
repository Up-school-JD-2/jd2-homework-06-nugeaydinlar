import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class ProductManager {

	private Map<String, Product> products; // veritabanı

	private Map<String, Supplier<String>> orderNumberSuppliers;

	private List<Order> orders;

	public ProductManager() {
		products = new HashMap<>();
		orderNumberSuppliers = new HashMap<>();
		orders = new ArrayList<>();
	}

	public void addProduct(Product product) {
		products.put(product.getId(), product);
	}

	public Product getProductById(String productId) {
		return products.get(productId);
	}

	public List<Product> filterProducts(Predicate<Product> filterPredicate) {
		// products.values().stream().filter(product->product.getName().startsWith("A")).toList();
		return products.values().stream().filter(filterPredicate).toList();
	}

	// BiConsumer
	// manager.updateStock("1", 20, (product, quantity) -> {
	// int newStock = product.getStock() + quantity;
	// product.setStock(newStock);
	// });
	public void updateStock(String productId, int quantity, BiConsumer<Product, Integer> updateFunction) {
		Product productById = getProductById(productId);
		if (productById != null) {
			updateFunction.accept(productById, quantity);
			System.out.println("Stock updated successfully");
		} else {
			System.out.println("Product not found");
		}
	}

	// calculate total value
	// double totalValue = manager.calculateTotalValue(product -> product.getPrice()
	// * product.getStock());
	public double calculateTotalValue(Function<Product, Double> valueFunction) {
		return products.values().stream().mapToDouble(valueFunction::apply).sum();
	}

	public void registerOrderNumberSupplier(String supplierId, Supplier<String> supplier) {
		orderNumberSuppliers.put(supplierId, supplier);
	}

	// generate Order number
	public String generateOrderNumber(String supplierId) {
		Supplier<String> supplier = orderNumberSuppliers.get(supplierId);
		if (supplier != null) {
			return supplier.get();
		} else {
			return "Supplier not found";
		}
	}

	// 0001 1 10
	// 0001 1 10
	// 0001 1 0
	// OrderItem -> String productId, Integer quantity;
	public void processOrder(String orderId, Map<String, Integer> orderItems,
			BiConsumer<Product, Integer> updateStockFunction) {
		Map<Product, Integer> productQuantityMap = new HashMap<>();
		for (Map.Entry<String, Integer> entry : orderItems.entrySet()) {
			String productId = entry.getKey();
			Integer quantity = entry.getValue();
			Product product = getProductById(productId);
			if (product != null) {
				updateStock(productId, quantity, updateStockFunction);
				productQuantityMap.compute(product, (key, value) -> {
					if (value == null) {
						return quantity;
					} else {
						return value + quantity;
					}
				});
			}
		}
		Order order = new Order(orderId, productQuantityMap);
		orders.add(order);

		System.out.println("Order processed successfully. Order ID: " + order.getOrderId());
		System.out.println("Ordered products:");
		order.getOrderDetails();
		System.out.println("Total Amount: " + order.getTotalAmount());
	}

	// ProductStatus'da ACTIVE olan urunleri fiyatlarina gore siralayip donduren
	// metodu yazin
	public List<Product> getActiveProductsSortedByPrice() {

		// Sadece ACTIVE durumunda olan urunler
		Predicate<Product> activeProductPredicate = product -> product.getProductStatus() == ProductStatus.ACTIVE;
		List<Product> activeProducts = filterProducts(activeProductPredicate);

		// Urunlerin fiyatlarina gore siralanmasi
		Comparator<Product> priceComparator = Comparator.comparing(Product::getPrice);
		List<Product> sortedProducts = activeProducts.stream().sorted(priceComparator).collect(Collectors.toList());

		return sortedProducts;
	}

	// String olarak verilen category'e ait olan urunlerin fiyatlarinin ortalamasini
	// yoksa 0.0 donduren metodu yazin
	// tip: OptionalDouble kullanimini inceleyin.

	public double calculateAveragePriceInCategory(String category) {
		// belirtilen kategoriye ait urunleri filtrelemek icin bir Predicate tanimlamasi
		Predicate<Product> categoryFilter = product -> product.getCategory().equals(category);

		// Filtrelenen urunlerin listeye kaydedilmesi
		List<Product> filteredProducts = filterProducts(categoryFilter);

		// Fiyatlarin toplanmasi icin DoubleStream
		DoubleStream priceStream = filteredProducts.stream().mapToDouble(Product::getPrice);

		// Ortalama fiyatin hesaplanmasi
		OptionalDouble averagePrice = priceStream.average();

		return averagePrice.orElse(0.0);
	}

	// category'lere gore gruplayip, her bir kategoride bulunan urunlerin toplam
	// fiyatini stream ile hesaplayip
	// donduren metodu yazin
	// urun:
	// category-1 105.2
	// category-2 45.0
	public Map<String, Double> getCategoryPriceSum() {

		// Urunlerin kategoriye gore gruplanmasi icin Function tanimlamasi
		Function<Product, String> categoryKeyExtractor = Product::getCategory;

		// Urunlerin kategoriye gore gruplanmasi icin Map olusturulmasi ve herbir
		// kategorinin toplam fiyatinin hesaplanmasi
		Map<String, Double> categoryPriceSum = products.values().stream()
				.collect(Collectors.groupingBy(categoryKeyExtractor, Collectors.summingDouble(Product::getPrice)));

		return categoryPriceSum;
	}
}

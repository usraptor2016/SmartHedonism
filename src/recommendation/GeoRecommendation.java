package recommendation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

public class GeoRecommendation {
	public List<Item> recommendItems(String userId,double lat,double lon) {
		List<Item> recommendItems = new ArrayList<>();

		// Step 1, get all favorited itemids
		DBConnection connection = DBConnectionFactory.getConnection();
		Set<String> favoritedItemIds = connection.getFavoriteItemIds(userId);

		// Step 2, get all categories,  sort by count
		// {"sports": 5, "music": 3, "art": 2}
		Map<String, Integer> allCategories = new HashMap<>();
		for (String itemId : favoritedItemIds) {
			Set<String> categories = connection.getCategories(itemId);
			for (String category : categories) {
				allCategories.put(category, allCategories.getOrDefault(category, 0) + 1);
			}
		}
		List<Map.Entry<String, Integer>> categoryList = new ArrayList<>(allCategories.entrySet());
		// uses lambda expression as a comparator
		Collections.sort(categoryList,
				         (Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) -> {
			return Integer.compare(e2.getValue(), e1.getValue());
		});

		// Step 3, search based on category, filter out favorite items
		Set<String> visitedItemIds = new HashSet<>();
		for (Map.Entry<String, Integer> category : categoryList) {
			List<Item> items = connection.searchItems(lat, lon, category.getKey());

			for (Item item : items) {
				if (!favoritedItemIds.contains(item.getItemId()) && !visitedItemIds.contains(item.getItemId())) {
					recommendItems.add(item);
					visitedItemIds.add(item.getItemId());
				}
			}
		}

		connection.close();
		return recommendItems;
	}

}

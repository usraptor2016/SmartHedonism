package db.mongodb;

import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import authentication.AuthUtil;
import authentication.PBKDF2;

import static com.mongodb.client.model.Filters.eq;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MongoDBConnection implements DBConnection {
	private MongoClient mongoClient;
	private MongoDatabase db;

	public MongoDBConnection() {
		mongoClient = MongoClients.create();
		db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	}
	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (db == null) {
			return;
		}
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$push", new Document("favorite", new Document("$each", itemIds))));

	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if (db == null) {
			return;
		}
		db.getCollection("users").updateOne(eq("user_id", userId),
				new Document("$pullAll", new Document("favorite", itemIds)));

	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<String> getFavoriteItemIds(String userId) {
		HashSet<String> favoriteSet = new HashSet<>();
		if (db == null) {
			return favoriteSet;
		}
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id",userId));
		if (iterable.first() != null && iterable.first().containsKey("favorite")) {
			List<String> list = (List<String>) iterable.first().get("favorite");
			favoriteSet.addAll(list);			
		}
		return favoriteSet;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		Set<Item> favoriteItemSet = new HashSet<>();
		if (db == null) {
			return favoriteItemSet;
		}
		Set<String> favoriteItemIds = getFavoriteItemIds(userId);
		for (String itemId:favoriteItemIds) {
			FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id",itemId));
			if (iterable.first() != null) {
				Document doc = iterable.first();
				
				ItemBuilder builder = new ItemBuilder();
				builder.setItemId(doc.getString("item_id"));
				builder.setName(doc.getString("name"));
				builder.setAddress(doc.getString("address"));
				builder.setUrl(doc.getString("url"));
				builder.setImageUrl(doc.getString("image_url"));
				builder.setRating(doc.getDouble("rating"));
				builder.setDistance(doc.getDouble("distance"));
				builder.setCategories(getCategories(itemId));
				
				favoriteItemSet.add(builder.build());
			}
		}
		return favoriteItemSet;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		if (db == null) {
			return new HashSet<>();
		}
		Set<String> categories = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
		
		if (iterable.first() != null && iterable.first().containsKey("categories")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("categories");
			categories.addAll(list);
		}

		return categories;


	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		TicketMasterAPI ticketMasterAPI = new TicketMasterAPI();
		List<Item> items = ticketMasterAPI.search(lat, lon, term);

		for(Item item : items) {
			saveItem(item);
		}

		return items;
	}

	@Override
	public void saveItem(Item item) {

    	if (db == null) {
    		return;
    	}
        try {
        	FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", item.getItemId()));
        	if (iterable.first() == null) {
        		db.getCollection("items")
        		.insertOne(new Document().append("item_id", item.getItemId()).append("distance", item.getDistance())
        				.append("name", item.getName()).append("address", item.getAddress())
        				.append("url", item.getUrl()).append("image_url", item.getImageUrl())
        				.append("rating", item.getRating()).append("categories", item.getCategories())
        				.append("lat", item.getLat()).append("lng", item.getLng())
        				);
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }


	}

	@Override
	public String getFullname(String userId) {
		String name = "";
		if (db == null) {
			return name;
		}
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id",userId));
		if (iterable.first() == null) {
			return name;
		}
		Document user = iterable.first();
		name = user.getString("first_name")+" "+user.getString("last_name");
		return name;
	}

	@Override
	public int verifyLogin(String userId, String password) {
		if (db == null) {
			System.err.println("DB connection failed");
			return 3;
		}
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id",userId));
		if (iterable.first() == null) {
			return 1;
		}
		Document user = iterable.first();
		String salt = user.getString("salt");
		String passwordHash = PBKDF2.hashPassword(password, salt);
		return passwordHash.equals(user.getString("password_hash"))?0:2;
	}

	@Override
	public boolean registerUser(String userId, String password, String firstname, String lastname) {
		if (db == null) {

			System.err.println("DB connection failed");
			return false;
		}
		
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id",userId));
		if (iterable.first() == null) {
			Document newUser = new Document();
			String salt = AuthUtil.generateSalt();
			String passwordHash = PBKDF2.hashPassword(password, salt);
			newUser.append("user_id", userId).append("first_name", firstname)
			       .append("last_name", lastname).append("salt", salt)
			       .append("password_hash", passwordHash);
			db.getCollection("users").insertOne(newUser);
			return true;
		}
		return false;
		
		
	}

}

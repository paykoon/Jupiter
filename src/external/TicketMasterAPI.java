package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;


public class TicketMasterAPI {
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			if (!embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				if (venues.length() > 0) {
					JSONObject venue = venues.getJSONObject(0);
					StringBuilder sb = new StringBuilder();
					
					if (!venue.isNull("address")) {
						JSONObject address = venue.getJSONObject("address");
						if (!address.isNull("line1")) {
							sb.append(address.getString("line1"));
						}
						if (!address.isNull("line2")) {
							sb.append(address.getString("line2"));
						}
						if (!address.isNull("line3")) {
							sb.append(address.getString("line3"));
						}
						sb.append(",");
					}
					if (!venue.isNull("city")) {
						JSONObject city = venue.getJSONObject("city");
						if (!city.isNull("name")) {
							sb.append(city.getString("name"));
						}
					}
					return sb.toString();
				}
			}
		}	
		return null;
	}

	// {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("images")) {
			JSONArray array = event.getJSONArray("images");
			for (int i = 0; i < array.length(); i++) {
				JSONObject image = array.getJSONObject(i);
				if (!image.isNull("url")) {
					return image.getString("url");
				}
			}
		}
		return null;
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (!event.isNull("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			for (int i = 0; i < classifications.length(); i++) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (!segment.isNull("name")) {
						String name = segment.getString("name");
						categories.add(name);
					}
				}
			}
		}
		return categories;
	}

	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			if (!event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			builder.setAddress(getAddress(event));
			
			Item item = builder.build();
			itemList.add(item);
		}
		
		return itemList;
	}

	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "Xx7zPmOXpDUTq4L1NrvZ6PXFJhKMqkKA";
	
    public List<Item> search(double lat, double lon, String keyword) {
        if (keyword == null) {
        		keyword = DEFAULT_KEYWORD;
        }
        try {
        		keyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        } catch (Exception e) {
        		e.printStackTrace();
        }
        String geoHash = GeoHash.encodeGeohash(lat, lon, 9);
        
        // String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);
        String query = String.format("apikey=" + API_KEY + "&geoPoint=" + geoHash + "&keyword=" + keyword + "&radius=" + 50);
        try {
        		// 创建和远端的http连接
        		// 等号右边的括号是为了强制把URL格式的转化成HttpURLConnection
        		HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
        		// 设置 request method
        		connection.setRequestMethod("GET");
        		// 自动发送请求，并接收返回相应的结果
        		// connection的发送设置已经在前面进行了
        		int responseCode = connection.getResponseCode();
        		// 如果没有问题，返回200
        		System.out.println("Response Code: "  + responseCode);
        		
        		// 准备拿出response的body
        		// 这里把自己当成client，inputstream是response. 
        		// BufferedReader 把response一行行读出来然后拼成大长串string response。
        		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        		StringBuilder response = new StringBuilder();
        		String inputLine = "";
        		while ((inputLine = in.readLine()) != null) {
        			response.append(inputLine);
        		}
        		in.close(); 
        		// 只能从string来建立JSONObject，所以对response进行强制转化
        		JSONObject obj = new JSONObject(response.toString());
        		// 判断这个key是否存在
        		// 不存在的话就返回一个空的JSONArray
        		// embedded是key, 找到其对应object
        		// events是key, 找到对应array
        		if (obj.isNull("_embedded")) {
    				// return new JSONArray();
        			return new ArrayList<>();
    			}
        		JSONObject embedded = obj.getJSONObject("_embedded");
    			JSONArray events = embedded.getJSONArray("events");
    			// return events;
    			return getItemList(events);
        } catch (Exception e) {
        		e.printStackTrace();
        }
        return new ArrayList<>();
    }
    
	private void queryAPI(double lat, double lon) {
		List<Item> events = search(lat, lon, null);
		try {
		    for (Item event : events) {
		    		// covert java object to JSON format
		    		JSONObject item = event.toJSONObject();
		    		System.out.println(item);
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX  
		tmApi.queryAPI(29.682684, -95.295410);
	}

}

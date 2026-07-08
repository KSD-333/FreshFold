package com.ankita.freshfold;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.ankita.freshfold.ui.home.MainActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationHelper {

    private static final String PREF_NAME = "freshfold_notifications_pref";
    private static final String KEY_NOTIFICATIONS = "notifications_json";
    private static final String CHANNEL_ID = "freshfold_order_channel";
    private static final String CHANNEL_NAME = "Order Status Notifications";

    public static class AppNotification {
        public String id;
        public String title;
        public String message;
        public long timestamp;
        public boolean isRead;
        public String orderId;
        public String addressDocId;

        public AppNotification(String id, String title, String message, long timestamp, boolean isRead, String orderId, String addressDocId) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
            this.isRead = isRead;
            this.orderId = orderId;
            this.addressDocId = addressDocId;
        }
    }

    // Trigger both System Status Bar Notification and save it as In-App Notification
    public static void triggerOrderSuccessNotification(Context context, String pickupDate, String serviceSummary, int totalPrice, String orderId, String addressDocId) {
        String title = "Order Placed Successfully! 🎉";
        String message = "Your order for " + serviceSummary + " (Total: ₹" + totalPrice + ") is scheduled for pickup on " + pickupDate + ".";
        
        // 1. Save locally for In-App Notifications
        addNotification(context, title, message, orderId, addressDocId);

        // 2. Trigger System Notification
        showSystemNotification(context, title, message);
    }

    // Save notification to SharedPreferences
    public static void addNotification(Context context, String title, String message, String orderId, String addressDocId) {
        List<AppNotification> list = getNotifications(context);
        String id = UUID.randomUUID().toString();
        AppNotification newNotif = new AppNotification(id, title, message, System.currentTimeMillis(), false, orderId, addressDocId);
        list.add(0, newNotif); // Add to the top of list
        saveNotifications(context, list);
    }

    // Get notifications list from SharedPreferences
    public static List<AppNotification> getNotifications(Context context) {
        List<AppNotification> list = new ArrayList<>();
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = pref.getString(KEY_NOTIFICATIONS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new AppNotification(
                        obj.getString("id"),
                        obj.getString("title"),
                        obj.getString("message"),
                        obj.getLong("timestamp"),
                        obj.getBoolean("isRead"),
                        obj.optString("orderId", ""),
                        obj.optString("addressDocId", "")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Check if there are any unread notifications
    public static boolean hasUnreadNotifications(Context context) {
        List<AppNotification> list = getNotifications(context);
        for (AppNotification n : list) {
            if (!n.isRead) {
                return true;
            }
        }
        return false;
    }

    // Mark all notifications as read
    public static void markAllAsRead(Context context) {
        List<AppNotification> list = getNotifications(context);
        for (AppNotification n : list) {
            n.isRead = true;
        }
        saveNotifications(context, list);
    }

    // Mark a specific notification as read
    public static void markAsRead(Context context, String notifId) {
        List<AppNotification> list = getNotifications(context);
        for (AppNotification n : list) {
            if (n.id.equals(notifId)) {
                n.isRead = true;
                break;
            }
        }
        saveNotifications(context, list);
    }

    private static void saveNotifications(Context context, List<AppNotification> list) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (AppNotification n : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", n.id);
                obj.put("title", n.title);
                obj.put("message", n.message);
                obj.put("timestamp", n.timestamp);
                obj.put("isRead", n.isRead);
                obj.put("orderId", n.orderId != null ? n.orderId : "");
                obj.put("addressDocId", n.addressDocId != null ? n.addressDocId : "");
                arr.put(obj);
            }
            pref.edit().putString(KEY_NOTIFICATIONS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Post to System Status Bar
    private static void showSystemNotification(Context context, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // Create Channel if Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("FreshFold Order Status Updates");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        // Click Action: Open MainActivity and route to NotificationActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_notifications", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Load logo
        Bitmap largeIcon = null;
        try {
            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_logo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // status bar small icon
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon); // use logo as large icon
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // Helper to convert document snapshot to Order
    public static Order documentSnapshotToOrder(com.google.firebase.firestore.DocumentSnapshot doc) {
        try {
            java.util.List<java.util.Map<String, Object>> itemsData = (java.util.List<java.util.Map<String, Object>>) doc.get("items");
            java.util.List<com.ankita.freshfold.CartItem> cartItems = new java.util.ArrayList<>();

            if (itemsData != null) {
                for (java.util.Map<String, Object> itemMap : itemsData) {
                    String name        = (String) itemMap.get("name");
                    String description = (String) itemMap.get("description");
                    int quantity       = itemMap.get("quantity") instanceof Long ? ((Long) itemMap.get("quantity")).intValue() : 0;
                    int pricePerUnit   = itemMap.get("pricePerUnit") instanceof Long ? ((Long) itemMap.get("pricePerUnit")).intValue() : 0;
                    String unit        = (String) itemMap.get("unit");
                    String emoji       = (String) itemMap.get("emoji");
                    String instructions = (String) itemMap.get("instructions");

                    com.ankita.freshfold.CartItem item = new com.ankita.freshfold.CartItem(
                        name != null ? name : "",
                        description != null ? description : "",
                        quantity, pricePerUnit,
                        unit != null ? unit : "",
                        emoji != null ? emoji : "",
                        instructions != null ? instructions : "",
                        0
                    );
                    cartItems.add(item);
                }
            }

            com.ankita.freshfold.Order order = new com.ankita.freshfold.Order(cartItems);
            order.setId(doc.getId());
            order.setDocumentPath(doc.getReference().getPath());
            if (doc.getString("status") != null) order.setStatus(doc.getString("status"));
            if (doc.getString("address") != null) order.setAddress(doc.getString("address"));

            Long ts = doc.getLong("timestamp");
            if (ts != null) order.setTimestamp(ts);

            return order;
        } catch (Exception e) {
            return null;
        }
    }
}

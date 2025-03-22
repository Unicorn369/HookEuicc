package cn.unicorn369;

import android.app.Activity;
import android.content.Intent;

public class Share {
    private static final String TAG = "Share";

    private Activity activity;

    private String text;

    private String title;

    private Share(Builder builder) {
        this.activity = builder.activity;
        this.text = builder.text;
        this.title = builder.title;
    }

    public void toShare() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.addCategory("android.intent.category.DEFAULT");

        shareIntent = Intent.createChooser(shareIntent, title);
        activity.startActivity(shareIntent);
    }

    public static class Builder {
        private Activity activity;
        private String text;
        private String title;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Share build() {
            return new Share(this);
        }

    }
}

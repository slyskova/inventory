package com.example.android.inventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.ProductContract.ProductEntry;
import com.example.android.inventory.data.ProductDbHelper;

public class ProductCursorAdapter extends CursorAdapter {

    private ProductDbHelper mDbHelper;

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        mDbHelper = new ProductDbHelper(context);

        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int descriptionColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

        final int id = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));
        String productName = cursor.getString(nameColumnIndex);
        String productDescription = cursor.getString(descriptionColumnIndex);
        int productPrice = cursor.getInt(priceColumnIndex);
        final int productQuantity = cursor.getInt(quantityColumnIndex);
        byte[] blob = cursor.getBlob(imageColumnIndex);

        if (TextUtils.isEmpty(productDescription)) {
            productDescription = context.getString(R.string.no_product_description);
        }

        TextView name = (TextView) view.findViewById(R.id.name);
        TextView description = (TextView) view.findViewById(R.id.description);
        TextView price = (TextView) view.findViewById(R.id.price);
        final TextView quantity = (TextView) view.findViewById(R.id.quantity);
        ImageView image = (ImageView) view.findViewById(R.id.image);

        name.setText(productName);
        description.setText(productDescription);
        price.setText(Integer.toString(productPrice));
        quantity.setText(Integer.toString(productQuantity));
        image.setImageBitmap(ImageUtils.getImage(blob));

        Button saleButton = (Button) view.findViewById(R.id.sale_button);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentQuantity = Integer.parseInt(quantity.getText().toString());

                if (currentQuantity > 0) {
                    int decreasedQuantity = currentQuantity - 1;
                    quantity.setText(String.valueOf(decreasedQuantity));
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, decreasedQuantity);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();
                    db.update(ProductEntry.TABLE_NAME, values, "_id=" + id, null);
                } else {
                    Toast.makeText(v.getContext(), "Unable to decrease quantity", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}




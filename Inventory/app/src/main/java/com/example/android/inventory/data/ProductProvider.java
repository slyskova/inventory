package com.example.android.inventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.android.inventory.ImageUtils;
import com.example.android.inventory.data.ProductContract.ProductEntry;

public class ProductProvider extends ContentProvider {

    private static final int PRODUCTS = 100;
    private static final int PRODUCT_ID = 101;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(ProductContract.CONTENT_AUTHORITY,
                ProductContract.PATH_PRODUCTS, PRODUCTS);
        sURIMatcher.addURI(ProductContract.CONTENT_AUTHORITY,
                ProductContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    private ProductDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sURIMatcher.match(uri);

        switch (match) {
            case PRODUCTS:
                Log.w("DEBUG", "selection: " + selection);
                Log.w("DEBUG", "selectionArgs: " + selectionArgs);
                cursor = db.query(ProductEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case PRODUCT_ID:
                Log.w("DEBUG", "selection: " + selection);
                Log.w("DEBUG", "selectionArgs: " + selectionArgs);
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(ProductEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown uri: " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sURIMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertProduct(Uri uri, ContentValues values) {
        Log.i("ProductProvider", "Values: " + values);
        String productName = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (productName == null) {
            throw new IllegalArgumentException("Product requires a name");
        }
        Integer productPrice = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
        if (productPrice != null && productPrice < 0) {
            throw new IllegalArgumentException("Product requires a correct price");
        }

        Integer productQuantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        if (productQuantity != null && productQuantity < 0) {
            throw new IllegalArgumentException("Product requires a correct quantity");
        }

        byte[] imageData = values.getAsByteArray(ProductEntry.COLUMN_PRODUCT_IMAGE);
        if (imageData == null) {
            throw new IllegalArgumentException("Product requires an image");
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(ProductEntry.TABLE_NAME, null, values);
        if (id == -1) {
            Log.e("ProductProvider", "Failed to insert row for: " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDeleted;
        final int match = sURIMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                rowsDeleted = db.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = db.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sURIMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String productName = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (productName == null) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            Integer productPrice = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
            if (productPrice != null && productPrice < 0) {
                throw new IllegalArgumentException("Product requires a correct price");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY)) {
            Integer productQuantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            if (productQuantity != null && productQuantity < 0) {
                throw new IllegalArgumentException("Product requires a correct quantity");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_IMAGE)) {
            byte[] imageData = values.getAsByteArray(ProductEntry.COLUMN_PRODUCT_IMAGE);
            ImageUtils.getImage(imageData);

            if (ImageUtils.getImage(imageData) == null) {
                throw new IllegalArgumentException("Product requires an image");
            }
        }

        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsUpdated = db.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowsUpdated == -1) {
            Log.e("ProductProvider", "Failed to update row for " + uri);
            return 0;
        } else if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}



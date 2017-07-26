package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.example.android.inventory.data.ProductContract.ProductEntry;

import java.io.ByteArrayOutputStream;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private static final int PICK_PHOTO_FOR_PRODUCT = 0;
    byte[] imageData;

    private boolean mProductHasChanged = false;

    private int mQuantity = 0;
    Bitmap bmp;

    private Uri mCurrentProductUri;

    private EditText mNameEditText;
    private EditText mDescriptionEditText;
    private EditText mQuantityEditText;
    private EditText mPriceEditText;
    private ImageView mProductImage;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mDescriptionEditText = (EditText) findViewById(R.id.edit_product_description);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        Button mOrderButton = (Button) findViewById(R.id.action_order_product);
        Button mDecreaseButton = (Button) findViewById(R.id.decrease_button);
        Button mIncreaseButton = (Button) findViewById(R.id.increase_button);
        mProductImage = (ImageView) findViewById(R.id.product_image);

        mNameEditText.setOnTouchListener(mTouchListener);
        mDescriptionEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mProductImage.setOnTouchListener(mTouchListener);
        mDecreaseButton.setOnTouchListener(mTouchListener);
        mIncreaseButton.setOnTouchListener(mTouchListener);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();
        Log.w("DEBUG", "mCurrentProductUri: " + mCurrentProductUri);

        if (mCurrentProductUri == null) {
            setTitle(R.string.title_add_product);
            mOrderButton.setVisibility(View.GONE);
        } else {
            setTitle(R.string.title_order_product);
            mOrderButton.setVisibility(View.VISIBLE);
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        mProductImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, PICK_PHOTO_FOR_PRODUCT);
            }
        });
    }


    public void incrementQuantity(View v) {
        if (!TextUtils.isEmpty(mQuantityEditText.getText().toString())) {
            mQuantity = Integer.parseInt(mQuantityEditText.getText().toString());
            mQuantityEditText.setText(String.valueOf(mQuantity + 1));
        } else {
            Toast.makeText(v.getContext(), "Enter quantity", Toast.LENGTH_SHORT).show();
        }
    }

    public void decrementQuantity(View v) {
        if (!TextUtils.isEmpty(mQuantityEditText.getText().toString())) {
            mQuantity = Integer.parseInt(mQuantityEditText.getText().toString());
            if (mQuantity > 0) {
                mQuantityEditText.setText(String.valueOf(mQuantity - 1));
            } else {
                Toast.makeText(v.getContext(), "Unable to decrease quantity", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(v.getContext(), "Enter quantity", Toast.LENGTH_SHORT).show();
        }
    }

    public void orderProduct(View v){
        int totalAmount = Integer.parseInt(mPriceEditText.getText().toString())
                * Integer.parseInt(mQuantityEditText.getText().toString());

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("message/rfc822");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Order extra " + mNameEditText.getText().toString());
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Dear seller \nI would like to order extra "
                + mNameEditText.getText().toString() + " from you. " +
                "Total amount of the order is going to be " + totalAmount + "$.\nThanks");
        startActivity(Intent.createChooser(sendIntent, "Email:"));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_IMAGE};

        return new CursorLoader(this,       // Parent activity context
                mCurrentProductUri,         // Provider content URI to query
                projection,                 // Columns to include in the resulting Cursor
                null,                       // No selection clause
                null,                       // No selection arguments
                null);                      // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int descriptionColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

            String name = cursor.getString(nameColumnIndex);
            String description = cursor.getString(descriptionColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            byte[] blob = cursor.getBlob(imageColumnIndex);

            mNameEditText.setText(name);
            mDescriptionEditText.setText(description);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Integer.toString(price));

            mProductImage.setImageBitmap(ImageUtils.getImage(blob));

            Log.v("DEBUG", "TEST: name = " + name + " description: "
                    + description + " quantity " + quantity + " price " + price);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mDescriptionEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mProductImage.setImageDrawable(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_save):
                saveProduct();
                return true;
            case (R.id.action_delete):
                showDeleteConfirmationDialog();
                return true;
            case (android.R.id.home):
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                } else {
                    DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NavUtils.navigateUpFromSameTask(EditorActivity.this);
                        }
                    };
                    showUnsavedChangesDialog(discardButtonClickListener);
                }
        }
        return true;
    }

    public void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_unsaved_changes);
        builder.setPositiveButton(R.string.dialog_leave_activity, discardButtonClickListener);
        builder.setNegativeButton(R.string.dialog_keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_delete_product);
        builder.setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void saveProduct() {
        String nameString = mNameEditText.getText().toString().trim();
        String descriptionString = mDescriptionEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        Bitmap bitmap = ((BitmapDrawable) mProductImage.getDrawable()).getBitmap();
        Bitmap emptyBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

        byte[] imageArray = ImageUtils.getImageBytes(bitmap);

        if (TextUtils.isEmpty(nameString) ||
                TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(priceString) ||
                bitmap.sameAs(emptyBitmap)) {
            Toast.makeText(this, R.string.editor_insert_all_product_info, Toast.LENGTH_SHORT).show();
            mNameEditText.setHint("Enter username");
            mNameEditText.setHintTextColor(getResources().getColor(R.color.colorAccent));
            mQuantityEditText.setHint("Enter quantity");
            mQuantityEditText.setHintTextColor(getResources().getColor(R.color.colorAccent));
            mPriceEditText.setHint("Enter price");
            mPriceEditText.setHintTextColor(getResources().getColor(R.color.colorAccent));
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, descriptionString);
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageArray);

        if (mCurrentProductUri == null) {
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
            if (newUri == null) {
                Toast.makeText(this, R.string.editor_insert_product_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.editor_insert_product_successful, Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowsAffected == 0) {
                Toast.makeText(this, "product failed",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Product saved",
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, R.string.editor_delete_product_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.editor_delete_product_successful, Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            if (requestCode == 0 && resultCode == RESULT_OK) {
                if (data != null) {
                    bmp = (Bitmap) data.getExtras().get("data");
                    mProductImage.setImageBitmap(bmp);
                    Log.d("camera ---- > ", "" + data.getExtras().get("data"));

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    imageData = baos.toByteArray();
                }
            } else {
                Uri selectedImage = data.getData();

                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();

                if (bmp != null && !bmp.isRecycled()) {
                    bmp = null;
                }

                bmp = BitmapFactory.decodeFile(filePath);
                mProductImage.setBackgroundResource(0);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                imageData = baos.toByteArray();
            }
        }
    }
}

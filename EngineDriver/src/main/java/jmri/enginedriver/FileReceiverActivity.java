package jmri.enginedriver;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileReceiverActivity extends AppCompatActivity {
    static final String activityName = "FileReceiverActivity";
    private threaded_application mainapp;

    private TextView textViewStatus;
    private TextView textViewFileNameDisplay;
    private ProgressBar progressBar;
    private ImageView imageViewStatus;
    private Button buttonClose;

    // Variable to store user's choice for overwriting files
    private boolean shouldOverwriteFiles = false; // Default to not overwriting

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainapp = (threaded_application) getApplication();
        mainapp.applyTheme(this);
        setContentView(R.layout.file_receiver);

        textViewStatus = findViewById(R.id.textViewFileReceiverStatus);
        textViewFileNameDisplay = findViewById(R.id.textViewFileName);
        progressBar = findViewById(R.id.progressBarFileReceiver);
        imageViewStatus = findViewById(R.id.imageViewStatus);
        buttonClose = findViewById(R.id.buttonCloseReceiver);

        buttonClose.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        textViewStatus.setText(getResources().getString(R.string.sharedFileProcessing));
        textViewFileNameDisplay.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        imageViewStatus.setVisibility(View.GONE);
        buttonClose.setVisibility(View.GONE);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {
                Log.d(threaded_application.applicationName, activityName + " Received single file URI: " + fileUri);
                String fileName = getFileName(fileUri, getContentResolver());
                textViewFileNameDisplay.setText(fileName);
                textViewFileNameDisplay.setVisibility(View.VISIBLE);

                if (doesFileExist(fileName)) {
                    showOverwriteChoiceDialog(
                            () -> saveFileAsync(fileUri, "shared_file_"),
                            () -> saveFileAsync(fileUri, "shared_file_")
                    );
                } else {
                    // No conflict, proceed to save directly (will not overwrite)
                    shouldOverwriteFiles = false; // Ensure it's false
                    saveFileAsync(fileUri, "shared_file_");
                }
            } else {
                Log.e(threaded_application.applicationName, activityName + " No URI found in SEND action");
                updateUiForError(getResources().getString(R.string.sharedFileNoUriReceived));
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            ArrayList<Parcelable> urisListParcelable = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (urisListParcelable != null && !urisListParcelable.isEmpty()) {
                ArrayList<Uri> urisList = new ArrayList<>();
                boolean conflictExists = false;
                for (Parcelable p : urisListParcelable) {
                    if (p instanceof Uri) {
                        Uri currentUri = (Uri) p;
                        urisList.add(currentUri);
                        String currentFileName = getFileName(currentUri, getContentResolver());
                        if (doesFileExist(currentFileName)) {
                            conflictExists = true;
                            // No need to check further if one conflict is found
                            // but we still need to populate urisList fully
                        }
                    }
                }

                if (!urisList.isEmpty()) {
                    Log.d(threaded_application.applicationName, "Processing " + urisList.size() + " files.");
                    textViewFileNameDisplay.setText(getResources().getString(R.string.sharedFileProcessingCount, urisList.size()));
                    textViewFileNameDisplay.setVisibility(View.VISIBLE);

                    if (conflictExists) {
                        showOverwriteChoiceDialog(
                                () -> saveMultipleFilesAsync(urisList, "shared_multiple_file_"),
                                () -> saveMultipleFilesAsync(urisList, "shared_multiple_file_")
                        );
                    } else {
                        // No conflicts, proceed to save directly (will not overwrite)
                        shouldOverwriteFiles = false; // Ensure it's false
                        saveMultipleFilesAsync(urisList, "shared_multiple_file_");
                    }
                } else {
                    Log.e(threaded_application.applicationName, "No valid URIs found in SEND_MULTIPLE parcelable list");
                    updateUiForError("Error: No valid file data received for multiple files.");
                }
            } else {
                Log.e(threaded_application.applicationName, "No URIs found in SEND_MULTIPLE action or list is empty");
                updateUiForError(getResources().getString(R.string.sharedFileNoUriReceived));
            }
        } else {
            Log.w(threaded_application.applicationName, "Activity launched without a valid share intent.");
            updateUiForError(getResources().getString(R.string.sharedFileNoShareActionDetected));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Clean up the executor service
    }

    // Helper method to check if a file with the given name already exists
    private boolean doesFileExist(String fileName) {
        File outputDir = getExternalFilesDir(null); // Or your specific target directory
        if (outputDir == null) {
            Log.w(threaded_application.applicationName, activityName + " Cannot get external files dir to check existence.");
            return false; // Safest to assume no conflict if dir is inaccessible
        }
        File potentialFile = new File(outputDir, fileName);
        return potentialFile.exists();
    }
    private void saveMultipleFilesAsync(ArrayList<Uri> uris, String filePrefix) {
        progressBar.setVisibility(View.VISIBLE);
        imageViewStatus.setVisibility(View.GONE);
        buttonClose.setVisibility(View.GONE);
        textViewStatus.setText(getResources().getString(R.string.sharedFileSavingCount,uris.size()));

        executorService.execute(() -> {
            int successCount = 0;
            int errorCount = 0;
            StringBuilder finalMessage = new StringBuilder();

            for (int i = 0; i < uris.size(); i++) {
                Uri uri = uris.get(i);
                String currentFileNameForDisplay = getFileName(uri, getContentResolver());
                // Update UI for current file being processed (optional, could make UI busy)
                // mainThreadHandler.post(() -> textViewFileNameDisplay.setText("Processing: " + currentFileNameForDisplay));

                String resultMessage = doSaveFileToScopedStorage(uri, filePrefix + (i + 1) + "_");
                if (!resultMessage.startsWith("Error")) {
                    successCount++;
                } else {
                    errorCount++;
                    Log.e(threaded_application.applicationName, activityName + "Error saving file " + currentFileNameForDisplay + ": " + resultMessage);
                }
            }

            if (errorCount == 0) {
                finalMessage.append(getResources().getString(R.string.sharedFileSavedCount,successCount));
            } else {
                finalMessage.append(getResources().getString(R.string.sharedFileSavedCount,successCount));
                finalMessage.append(getResources().getString(R.string.sharedFileFailedToSaveCount,errorCount));
                // You might want to provide more details about errors if possible,
                // e.g., by collecting individual error messages.
            }

            boolean overallSuccess = errorCount == 0;
            String finalResultMessage = finalMessage.toString();

            mainThreadHandler.post(() -> {
                textViewFileNameDisplay.setText(getResources().getString(R.string.sharedFileFinished, uris.size()));
                if (overallSuccess) {
                    updateUiForSuccess(finalResultMessage);
                } else {
                    updateUiForError(finalResultMessage);
                }
            });
        });
    }

    private void saveFileAsync(Uri uri, String filePrefix) {
        progressBar.setVisibility(View.VISIBLE);
        imageViewStatus.setVisibility(View.GONE); // Hide status icon during progress
        buttonClose.setVisibility(View.GONE);   // Hide close button during progress
        textViewStatus.setText(getResources().getString(R.string.sharedFileSavingFile)); // Update status text

        executorService.execute(() -> {
            String resultMessage = doSaveFileToScopedStorage(uri, filePrefix); // Renamed internal method
            boolean success = !resultMessage.startsWith("Error");

            mainThreadHandler.post(() -> {
                if (success) {
                    updateUiForSuccess(resultMessage);
                } else {
                    updateUiForError(resultMessage);
                }
            });
        });
    }

    private void updateUiForSuccess(String message) {
        progressBar.setVisibility(View.GONE);
        imageViewStatus.setVisibility(View.VISIBLE);
        // Make sure you have ic_check_circle_green or similar in res/drawable
        imageViewStatus.setImageResource(R.drawable.tick_glyph_large_dark);
        // Optional: Tint the image view if your drawable is a generic white icon
        // imageViewStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark), android.graphics.PorterDuff.Mode.SRC_IN);
        textViewStatus.setText(message);
        buttonClose.setVisibility(View.VISIBLE);
    }

    private void updateUiForError(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        imageViewStatus.setVisibility(View.VISIBLE);
        // Make sure you have ic_error_red or similar in res/drawable
        imageViewStatus.setImageResource(R.drawable.cross_glyph_dark);
        // Optional: Tint the image view
        // imageViewStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.SRC_IN);
        textViewStatus.setText(errorMessage);
        buttonClose.setVisibility(View.VISIBLE);
        // threaded_application.safeToast(getApplicationContext(), errorMessage, Toast.LENGTH_LONG);
    }

    private String doSaveFileToScopedStorage(Uri uri, String filePrefix) {
        ContentResolver contentResolver = getContentResolver();
        String originalFileName = getFileName(uri, contentResolver); // Keep original for display/logging

        File outputDir = getExternalFilesDir(null);
        if (outputDir == null) {
            Log.e(threaded_application.applicationName, activityName + "Failed to get app-specific external storage directory.");
            return "Error: Cannot access storage.";
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(threaded_application.applicationName, activityName + "Failed to create app-specific directory.");
            return "Error: Cannot create storage directory.";
        }

        File outputFile = new File(outputDir, originalFileName);
        String finalFileNameToSave = originalFileName; // This will be the name used for saving

        if (!shouldOverwriteFiles) { // Only try to make unique if not overwriting
            int count = 0;
            String baseName = originalFileName.lastIndexOf('.') > 0 ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : originalFileName;
            String extension = originalFileName.lastIndexOf('.') > 0 ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";

            while (outputFile.exists()) {
                count++;
                finalFileNameToSave = baseName + "_" + count + extension;
                outputFile = new File(outputDir, finalFileNameToSave);
            }
        }
        // If shouldOverwriteFiles is true, outputFile will point to the original name,
        // and FileOutputStream will overwrite it if it exists.

        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(outputFile)) { // outputFile uses finalFileNameToSave or originalFileName
            if (inputStream == null) {
                Log.e(threaded_application.applicationName, activityName + "Failed to open input stream for URI: " + uri);
                return "Error: Could not read shared file (" + originalFileName + ").";
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.i(threaded_application.applicationName, activityName + "File saved successfully: " + outputFile.getAbsolutePath());
            return "File saved: " + finalFileNameToSave; // Display the name it was actually saved as
        } catch (IOException e) {
            Log.e(threaded_application.applicationName, activityName + "Error saving file: " + originalFileName, e);
            return "Error saving " + originalFileName + ": " + e.getMessage();
        } catch (SecurityException e) {
            Log.e(threaded_application.applicationName, activityName + "Security exception, URI permission issue? " + uri, e);
            return "Error: Permission denied for " + originalFileName + ".";
        }
    }

    // Helper method to get the file name from a content URI
    private String getFileName(Uri uri, ContentResolver contentResolver) {
        String defaultName = "unknown_";
        String fileName = null;
        if (uri == null) return defaultName;

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (android.database.Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex);
                        if (fileName != null && !fileName.isEmpty()) {
                            // Basic sanitization
                            return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(threaded_application.applicationName, activityName + "Error getting filename from content URI", e);
            }
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = uri.getLastPathSegment();
            if (fileName != null && !fileName.isEmpty()) {
                // Basic sanitization
                return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            }
        }

        if (fileName == null || fileName.isEmpty()) {
            String mimeType = contentResolver.getType(uri);
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            fileName = defaultName + "_" + System.currentTimeMillis() + (extension != null ? "." + extension : ".dat");
        }
        // Final sanitization
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void showOverwriteChoiceDialog(Runnable onConfirmNoOverwrite, Runnable onConfirmOverwrite) {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.sharedFileOverwriteChoiceTitle))
                .setMessage(getResources().getString(R.string.sharedFileOverwriteChoiceText))
                .setPositiveButton(getResources().getString(R.string.sharedFileOverwriteChoicePositiveButtonText), (dialog, which) -> {
                    shouldOverwriteFiles = true;
                    onConfirmOverwrite.run(); // Proceed with saving, allowing overwrites
                })
                .setNegativeButton(getResources().getString(R.string.sharedFileOverwriteChoiceNegativeButtonText), (dialog, which) -> {
                    shouldOverwriteFiles = false;
                    onConfirmNoOverwrite.run(); // Proceed with saving, creating unique names
                })
                .setCancelable(false) // User must make a choice
                .show();
    }

}

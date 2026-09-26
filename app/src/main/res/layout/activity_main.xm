<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="32dp"
    android:background="#FAFAFA">

    <ImageView
        android:layout_width="96dp"
        android:layout_height="96dp"
        android:src="@android:drawable/ic_popup_sync"
        android:layout_marginBottom="24dp"
        android:contentDescription="sync" />

    <TextView
        android:id="@+id/tvStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/checking"
        android:textSize="18sp"
        android:textColor="#212121"
        android:layout_marginBottom="16dp" />

    <ProgressBar
        android:id="@+id/progressBar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="8dp"
        android:max="100"
        android:progress="0"
        android:layout_marginBottom="24dp" />

    <TextView
        android:id="@+id/tvSubStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/please_wait"
        android:textSize="13sp"
        android:textColor="#757575" />

</LinearLayout>

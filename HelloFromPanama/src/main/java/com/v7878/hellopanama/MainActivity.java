package com.v7878.hellopanama;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.hellopanama.databinding.ActivityMainBinding;

import java.lang.invoke.MethodHandle;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'panamaexamples' library on application startup.
    static {
        System.loadLibrary("hellopanama");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Linker linker = Linker.nativeLinker();
        // Native symbols from current classloader
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        FunctionDescriptor descriptor = FunctionDescriptor.of(
                ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, JAVA_BYTE)));

        MethodHandle getter = linker.downcallHandle(lookup.findOrThrow("getString"), descriptor);
        String hello;
        try {
            hello = ((MemorySegment) getter.invoke()).getString(0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        TextView tv = binding.sampleText;
        tv.setText(hello);
    }
}
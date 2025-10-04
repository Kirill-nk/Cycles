package com.example.myapplication;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddPersonActivity extends AppCompatActivity {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);

        EditText inputName = findViewById(R.id.input_name);
        EditText inputAge = findViewById(R.id.input_age);
        EditText inputLast = findViewById(R.id.input_last_date);
        EditText inputPrev = findViewById(R.id.input_prev_date);
        Button btnSave = findViewById(R.id.btn_save);

        // Allow Latin and Cyrillic letters, spaces and dashes in name
        inputName.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (!(Character.isLetter(c) || c == ' ' || c == '-')) {
                        return "";
                    }
                }
                return null;
            }
        }});

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inputName.getText().toString().trim();
                String ageStr = inputAge.getText().toString().trim();
                String last = inputLast.getText().toString().trim();
                String prev = inputPrev.getText().toString().trim();
                String daysStr = "";

                if (name.isEmpty()) {
                    Toast.makeText(AddPersonActivity.this, "Имя", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!last.isEmpty() && !isValidDate(last)) {
                    Toast.makeText(AddPersonActivity.this, "Неверный формат даты последней менструации", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!prev.isEmpty() && !isValidDate(prev)) {
                    Toast.makeText(AddPersonActivity.this, "Неверный формат предыдущей даты", Toast.LENGTH_SHORT).show();
                    return;
                }

                Integer age = null;
                try {
                    if (!ageStr.isEmpty()) age = Integer.parseInt(ageStr);
                } catch (NumberFormatException ignore) { }

                Integer daysToOvulation = null;

                Person p = new Person(name,
                        last.isEmpty() ? null : last,
                        prev.isEmpty() ? null : prev,
                        daysToOvulation,
                        age);

                new Thread(() -> {
                    AppDatabase.getInstance(AddPersonActivity.this).personDao().insert(p);
                    runOnUiThread(() -> {
                        Toast.makeText(AddPersonActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }).start();
            }
        });
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value, dateFormatter);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}


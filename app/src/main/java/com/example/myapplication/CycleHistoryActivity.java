package com.example.myapplication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CycleHistoryActivity extends AppCompatActivity {

    private Person person;
    private List<String> cycleDates = new ArrayList<>();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private LinearLayout datesContainer;
    private Button addDateButton;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_history);

        // Получаем данные о человеке из Intent
        long personId = getIntent().getLongExtra("person_id", -1);
        if (personId == -1) {
            Toast.makeText(this, R.string.error_cannot_get_data, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Загружаем данные о человеке
        loadPersonData(personId);

        // Инициализируем UI
        initUI();
    }

    private void loadPersonData(long personId) {
        new Thread(() -> {
            person = AppDatabase.getInstance(this).personDao().getById(personId);
            runOnUiThread(() -> {
                if (person != null) {
                    TextView nameTitle = findViewById(R.id.tv_person_name);
                    nameTitle.setText(person.name);
                    // Загружаем существующие даты после загрузки person
                    loadCycleDates();
                }
            });
        }).start();
    }

    private void initUI() {
        datesContainer = findViewById(R.id.ll_dates_container);
        addDateButton = findViewById(R.id.btn_add_date);
        saveButton = findViewById(R.id.btn_save);

        addDateButton.setOnClickListener(v -> showDatePicker());
        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void loadCycleDates() {
        // Очищаем контейнер
        datesContainer.removeAllViews();

        // Проверяем, что person загружен
        if (person == null) return;

        // Очищаем список дат и добавляем существующие
        cycleDates.clear();

        // Добавляем существующие даты
        if (person.lastCycleStartDate != null && !person.lastCycleStartDate.isEmpty()) {
            cycleDates.add(person.lastCycleStartDate);
            addDateView(person.lastCycleStartDate, false);
        }
        if (person.previousCycleStartDate != null && !person.previousCycleStartDate.isEmpty()) {
            cycleDates.add(person.previousCycleStartDate);
            addDateView(person.previousCycleStartDate, false);
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                String dateString = selectedDate.format(dateFormatter);
                cycleDates.add(dateString);
                addDateView(dateString, true);
            },
            LocalDate.now().getYear(),
            LocalDate.now().getMonthValue() - 1,
            LocalDate.now().getDayOfMonth()
        );
        datePickerDialog.show();
    }

    private void addDateView(String date, boolean isRemovable) {
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView dateTextView = new TextView(this);
        dateTextView.setText(date);
        dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ));
        dateTextView.setTextSize(16);
        dateTextView.setPadding(16, 16, 16, 16);

        dateRow.addView(dateTextView);

        if (isRemovable) {
            Button removeButton = new Button(this);
            removeButton.setText("✕");
            removeButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            removeButton.setOnClickListener(v -> {
                cycleDates.remove(date);
                datesContainer.removeView(dateRow);
            });
            dateRow.addView(removeButton);
        }

        datesContainer.addView(dateRow);
    }

    private void saveChanges() {
        if (cycleDates.isEmpty()) {
            Toast.makeText(this, R.string.add_at_least_one_date, Toast.LENGTH_SHORT).show();
            return;
        }

        // Сортируем даты по убыванию (новые сначала)
        cycleDates.sort((d1, d2) -> {
            LocalDate date1 = parseDateOrNull(d1);
            LocalDate date2 = parseDateOrNull(d2);
            if (date1 == null || date2 == null) return 0;
            return date2.compareTo(date1);
        });

        // Обновляем данные в базе
        new Thread(() -> {
            if (cycleDates.size() >= 1) {
                person.lastCycleStartDate = cycleDates.get(0);
            }
            if (cycleDates.size() >= 2) {
                person.previousCycleStartDate = cycleDates.get(1);
            }

            AppDatabase.getInstance(this).personDao().update(person);
            
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.changes_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private LocalDate parseDateOrNull(String value) {
        if (TextUtils.isEmpty(value)) return null;
        try {
            return LocalDate.parse(value, dateFormatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
} 
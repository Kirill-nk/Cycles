package com.example.myapplication;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Person;
import com.example.myapplication.CycleHistoryActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enable home button in ActionBar to be clickable
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false); // Remove back arrow
            actionBar.setDisplayShowTitleEnabled(true);
            // Make the title clickable
            actionBar.setTitle("girls");
        }

        Button showResults = findViewById(R.id.btn_show_results);
        Button addPerson = findViewById(R.id.btn_add_person);
        TableLayout table = findViewById(R.id.table_results);

        showResults.setOnClickListener(v -> loadAndRenderResults(table));
        addPerson.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPersonActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        // Add a custom menu item that acts as a clickable title
        android.view.MenuItem titleItem = menu.add(android.view.Menu.NONE, android.R.id.home, 0, "Девчули");
        titleItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Refresh the main screen data when title is clicked
            TableLayout table = findViewById(R.id.table_results);
            loadAndRenderResults(table);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TableLayout table = findViewById(R.id.table_results);
        loadAndRenderResults(table);
    }

    private void loadAndRenderResults(TableLayout table) {
        new Thread(() -> {
            List<Person> people = AppDatabase.getInstance(this).personDao().getAll();
            runOnUiThread(() -> {
                // Clear all rows except header (index 0)
                while (table.getChildCount() > 1) {
                    table.removeViewAt(1);
                }
                
                // Sort people by next period date in ascending order
                people.sort((p1, p2) -> {
                    LocalDate last1 = parseDateOrNull(p1.lastCycleStartDate);
                    LocalDate prev1 = parseDateOrNull(p1.previousCycleStartDate);
                    LocalDate last2 = parseDateOrNull(p2.lastCycleStartDate);
                    LocalDate prev2 = parseDateOrNull(p2.previousCycleStartDate);
                    
                    LocalDate next1 = calculateNextPeriod(last1, prev1);

                    if (next1 == null && next2 == null) return 0;
                    if (next1 == null) return 1;
                    if (next2 == null) return -1;
                    
                    return next1.compareTo(next2);
                });
                
                for (Person person : people) {
                    String name = person.name != null ? person.name.trim() : "";
                    LocalDate last = parseDateOrNull(person.lastCycleStartDate);
                    LocalDate prev = parseDateOrNull(person.previousCycleStartDate);

                    String fertileWindowText = "—";
                    String nextPeriodText = "—";

                    LocalDate next = calculateNextPeriod(last, prev);
                    if (next != null) {
                        nextPeriodText = next.format(dateFormatter);
                        
                        // Ovulation ~14 days before next period, show single average date
                        LocalDate ovulation = next.minusDays(14);
                        fertileWindowText = ovulation.format(dateFormatter);
                    }

                    TableRow row = new TableRow(this);
                    TextView nameView = new TextView(this);
                    nameView.setText(name);
                    nameView.setClickable(true);
                    nameView.setFocusable(true);
                    nameView.setBackgroundResource(android.R.drawable.list_selector_background);
                    nameView.setPadding(16, 16, 16, 16);
                    
                    // Добавляем клик-листенер для имени
                    nameView.setOnClickListener(v -> {
                        Intent intent = new Intent(this, CycleHistoryActivity.class);
                        intent.putExtra("person_id", person.id);
                        startActivity(intent);
                    });
                    
                    TextView lastPeriodView = new TextView(this);
                    lastPeriodView.setText(last != null ? last.format(dateFormatter) : "—");
                    lastPeriodView.setPadding(16, 16, 16, 16);

                    TextView fertileView = new TextView(this);
                    fertileView.setText(fertileWindowText);
                    fertileView.setPadding(16, 16, 16, 16);
                    
                    TextView nextPeriodView = new TextView(this);
                    nextPeriodView.setText(nextPeriodText);
                    nextPeriodView.setPadding(16, 16, 16, 16);
                    
                    TextView daysToFertileView = new TextView(this);
                    String daysToFertileText = "—";
                    if (next != null) {
                        LocalDate ovulation = next.minusDays(14);
                        long daysToFertile = ChronoUnit.DAYS.between(LocalDate.now(), ovulation);
                        if (daysToFertile >= 0) {
                            daysToFertileText = String.valueOf(daysToFertile);
                        } else {
                            daysToFertileText = "0";
                        }
                    }
                    daysToFertileView.setText(daysToFertileText);
                    daysToFertileView.setPadding(16, 16, 16, 16);

                    TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                    nameView.setLayoutParams(lp);
                    lastPeriodView.setLayoutParams(lp);
                    fertileView.setLayoutParams(lp);
                    nextPeriodView.setLayoutParams(lp);
                    daysToFertileView.setLayoutParams(lp);

                    // Enable editing last period date by tapping its cell
                    lastPeriodView.setClickable(true);
                    lastPeriodView.setFocusable(true);
                    lastPeriodView.setBackgroundResource(android.R.drawable.list_selector_background);
                    lastPeriodView.setOnClickListener(v -> {
                        LocalDate initDate = last != null ? last : LocalDate.now();
                        DatePickerDialog picker = new DatePickerDialog(
                                this,
                                (view, year, month, dayOfMonth) -> {
                                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                                    String newDate = selected.format(dateFormatter);
                                    new Thread(() -> {
                                        person.lastCycleStartDate = newDate;
                                        AppDatabase.getInstance(this).personDao().update(person);
                                        runOnUiThread(() -> {
                                            Toast.makeText(this, "Дата обновлена", Toast.LENGTH_SHORT).show();
                                            loadAndRenderResults(table);
                                        });
                                    }).start();
                                },
                                initDate.getYear(),
                                initDate.getMonthValue() - 1,
                                initDate.getDayOfMonth()
                        );
                        picker.show();
                    });

                    row.addView(nameView);
                    row.addView(lastPeriodView);
                    row.addView(fertileView);
                    row.addView(nextPeriodView);
                    row.addView(daysToFertileView);
                    table.addView(row);
                }
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

    private LocalDate calculateNextPeriod(LocalDate lastCycleStartDate, LocalDate previousCycleStartDate) {
        if (lastCycleStartDate == null || previousCycleStartDate == null) {
            return null;
        }

        // Calculate the length of the current cycle
        long cycleLength = ChronoUnit.DAYS.between(previousCycleStartDate, lastCycleStartDate);

        // If the cycle length is 0 or negative, it means the last cycle was too short or invalid.
        // In this case, we cannot reliably calculate the next period.
        if (cycleLength <= 0) {
            return null;
        }

        // Calculate the next period date
        LocalDate nextPeriod = lastCycleStartDate.plusDays(cycleLength);
        return nextPeriod;
    }
}


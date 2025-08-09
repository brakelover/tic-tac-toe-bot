package vn.edu.fpt.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn3x3 = findViewById(R.id.btn3x3);
        Button btn5x5 = findViewById(R.id.btn5x5);
        Button btn10x10 = findViewById(R.id.btn10x10);
        Button btnCustomMode = findViewById(R.id.btnCustomMode);
        Switch switchAI = findViewById(R.id.switchAI);
        switchAI.setChecked(true);

        btn3x3.setOnClickListener(v -> startGame(3, 3, switchAI.isChecked()));
        btn5x5.setOnClickListener(v -> startGame(5, 5, switchAI.isChecked()));
        btn10x10.setOnClickListener(v -> startGame(10, 10, switchAI.isChecked()));
        btnCustomMode.setOnClickListener(v -> showCustomDialog());
    }

    private void startGame(int rows, int columns, boolean isAI) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("rows", rows);
        intent.putExtra("columns", columns);
        intent.putExtra("isAI", isAI);  // 🔑 Thêm AI vào intent
        startActivity(intent);
        finish();
    }

    private void showCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Chọn tỉ lệ bàn cờ");

        View customView = getLayoutInflater().inflate(R.layout.dialog_custom_mode, null);
        builder.setView(customView);

        EditText edtRow = customView.findViewById(R.id.edtRow);
        EditText edtColumn = customView.findViewById(R.id.edtColumn);
        Button btnOK = customView.findViewById(R.id.btnOK);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        btnOK.setOnClickListener(v -> {
            String row = edtRow.getText().toString();
            String column = edtColumn.getText().toString();

            if (!row.isEmpty() && !column.isEmpty()) {
                int rows = Integer.parseInt(row);
                int columns = Integer.parseInt(column);

                if (rows >= 3 && columns >= 3 && rows <= 20 && columns <= 20) {
                    Intent intent = new Intent(MainActivity.this, GameActivity.class);
                    intent.putExtra("rows", rows);
                    intent.putExtra("columns", columns);
                    startActivity(intent);
                    dialog.dismiss();
                }
                else {
                    Toast.makeText(this, "Hàng và cột phải lớn hơn hoặc bằng 3 và nhỏ hơn hoặc bằng 20!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

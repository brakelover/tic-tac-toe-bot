package vn.edu.fpt.tic_tac_toe;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameActivity extends AppCompatActivity {
    private int rows; // Số hàng của bảng
    private int columns; // Số cột của bảng
    private Button[][] buttons; // Mảng 2 chiều lưu các nút trên bảng
    private boolean playerX = true; // Biến kiểm tra lượt chơi (true: X, false: O)
    private int winCondition; // Điều kiện thắng (số ô liên tiếp cần thiết)
    private boolean isAI; // Kiểm tra chế độ chơi với máy
    private int lastXRow = -1; // Lưu vị trí hàng cuối cùng của "X"
    private int lastXCol = -1; // Lưu vị trí cột cuối cùng của "X"
    private HashSet<Point> blockedPositions = new HashSet<>(); // Lưu các thế đã bắt gặp chặn
    private List<int[]> winningPositions = new ArrayList<>(); // highlight vị trí thắng
    private boolean gameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Xử lý nút "Quay lại"
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Kết thúc GameActivity để tránh quay lại
        });

        // Lấy số hàng và số cột từ Intent
        rows = getIntent().getIntExtra("rows", 3);
        columns = getIntent().getIntExtra("columns", 3);
        isAI = getIntent().getBooleanExtra("isAI", true);

        // Hiển thị thông báo chế độ chơi
        if (isAI) {
            Toast.makeText(this, "Chế độ chơi với máy", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Chế độ hai người chơi", Toast.LENGTH_SHORT).show();
        }

        // Thiết lập GridLayout
        GridLayout gridLayout = findViewById(R.id.gridLayout);
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(columns);
        gridLayout.setUseDefaultMargins(true);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        // Khởi tạo mảng các nút
        buttons = new Button[rows][columns];

        // Điều kiện thắng (3 ô liên tiếp cho 3x3, 5 ô liên tiếp cho 5x5 trở lên)
        winCondition = Math.min(Math.min(rows, columns), 5);

        // Tạo các nút và thêm vào GridLayout
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Button button = new Button(this);
                button.setText("");
                button.setOnClickListener(this::onCellClick); // Gán sự kiện click
                button.setBackgroundResource(android.R.drawable.btn_default);
                button.setPadding(0, 0, 0, 0);
                button.setGravity(Gravity.CENTER);
                button.setTypeface(null, Typeface.BOLD); // Chữ đậm

                // Thiết lập layout cho nút
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.setMargins(2, 2, 2, 2); // Thêm viền cho ô không dính vào nhau
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(i, GridLayout.FILL, 1f); // Cho phép co giãn theo tỉ lệ
                params.columnSpec = GridLayout.spec(j, GridLayout.FILL, 1f); // Cho phép co giãn theo tỉ lệ
                button.setLayoutParams(params);

                // Thiết lập kích thước chữ dựa trên kích thước nút
                button.post(() -> {
                    int size = Math.min(button.getWidth(), button.getHeight());
                    // Set text size 95% chiều rộng ô
                    float textSize = size * 0.95f * 0.5f; // 0.95 để lấy 95%, 0.5 để chuyển px -> sp
                    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                });

                gridLayout.addView(button); // Thêm nút vào GridLayout
                buttons[i][j] = button; // Lưu nút vào mảng

            }
        }
    }

    // Xử lý sự kiện khi người chơi nhấn vào một ô
    private void onCellClick(View v) {
        // Bỏ nhấn nếu trò chơi kết thúc
        if (gameOver) return;

        Button button = (Button) v;
        // Nếu ô đã được chọn hoặc là lượt bot thì không làm gì cả
        if (!button.getText().toString().equals("") || (isAI && !playerX)) return;

        // Đánh dấu "X" hoặc "O" tùy lượt chơi
        button.setText(playerX ? "X" : "O");

        // Cập nhật vị trí cuối cùng của "X"
        if (playerX) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (buttons[i][j] == button) {
                        lastXRow = i;
                        lastXCol = j;
                        break;
                    }
                }
            }
        }

        // Kiểm tra thắng trước khi đổi lượt
        if (checkWin()) {
            return;
        }

        playerX = !playerX; // Đổi lượt chơi

        // Phát âm thanh khi đánh
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sound);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
        }); // Giải phóng sau khi phát âm thanh, tránh rò rỉ bộ nhớ

        // Kiểm tra thắng sau khi đổi lượt
        if (checkWin()) {
            String winner = playerX ? "O thắng!" : "X thắng!";
            showResultDialog(winner);
            return;
        }

        checkDraw(); // Kiểm tra hòa

        // Nếu là chế độ chơi với máy và đến lượt bot, gọi botMove sau 1 giây
        if (isAI && !playerX) {
            new Handler().postDelayed(this::botMove, 1000);
        }
    }

    // Hàm xử lý lượt đi của bot
    private void botMove() {
        if (!isAI || playerX) return; // Chỉ đánh khi là lượt của bot và đang chơi với AI

        // Kiểm tra nếu game đã kết thúc thì không đánh nữa
        if (checkWin() || checkDraw()) return;

        // 1. Nếu bot có thể thắng, đánh ngay
        if (findWinningMove("O")) {
            if (checkWin()) return; // Kiểm tra thắng sau khi bot đặt quân
        }

        // 2. Nếu không, chặn nước thắng của người chơi
        if (findBlockingMove("X", "O")) {
            playerX = true; // Chuyển lượt về người chơi sau khi bot chặn
            checkDraw(); // Kiểm tra hòa sau khi bot chặn
            return;
        }

        // 3. Ưu tiên chặn dãy 3 "X" hoặc hoàn thiện dãy 3 "O"
        if (findAndBlockThreat()) {
            playerX = true;
            checkDraw();
            return;
        }

        // 4. Đánh xung quanh vị trí cuối cùng của "X"
        if (lastXRow != -1 && lastXCol != -1) {
            List<int[]> availableMoves = new ArrayList<>();

            // Kiểm tra các ô xung quanh (bao gồm cả chéo)
            for (int i = lastXRow - 1; i <= lastXRow + 1; i++) {
                for (int j = lastXCol - 1; j <= lastXCol + 1; j++) {
                    if (i >= 0 && i < rows && j >= 0 && j < columns && buttons[i][j].getText().toString().equals("")) {
                        availableMoves.add(new int[]{i, j});
                    }
                }
            }

            // Nếu có ô trống xung quanh, chọn ngẫu nhiên một ô để đánh
            if (!availableMoves.isEmpty()) {
                Random random = new Random();
                int[] move = availableMoves.get(random.nextInt(availableMoves.size()));
                int i = move[0];
                int j = move[1];

                buttons[i][j].setText("O");

                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sound);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);

                if (checkWin()) return;
                playerX = true;
                checkDraw();
                return;
            }
        }


        // 5. Đánh vào vị trí chiến lược (trung tâm hoặc góc)
        int[] strategicMove = findStrategicMove();
        if (strategicMove != null) {
            int i = strategicMove[0];
            int j = strategicMove[1];
            buttons[i][j].setText("O");

            // Phát âm thanh
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sound);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);

            if (checkWin()) return;
            playerX = true;
            checkDraw();
            return;
        }

        // 6. Đánh ngẫu nhiên vào ô trống nếu không có nước đi nào khác
        Random random = new Random();
        int i, j;
        do {
            i = random.nextInt(rows);
            j = random.nextInt(columns);
        } while (!buttons[i][j].getText().toString().equals(""));

        buttons[i][j].setText("O");

        // Phát âm thanh
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sound);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);

        // Kiểm tra thắng hoặc hòa sau khi bot đi
        if (checkWin()) return;
        playerX = true; // Chuyển lượt về người chơi
        checkDraw();
    }

    // Hàm tìm và chặn mối đe dọa theo thứ tự ưu tiên
    public boolean findAndBlockThreat() {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}; // 8 hướng từ {0, 0}

        // **Danh sách mẫu theo thứ tự ưu tiên
        // 1, Chặn hết X còn 1 nước win
        // 2, Tấn công đủ 4O
        // 3, Chặn hết X còn 2 nước win
        // 4, Tấn công khi có 2 nước có thể đi
        // 5, Chặn các nước đôi của X, nếu mã thêm đuôi O là O tấn công nước đôi
        //     SWES a.k.a Square Without Edges: là hình vuông nhưng chỉ có 4 điểm, và khi chèn O vào ruột thì sẽ thành dấu X lớn với điểm giao O
        //     DMND a.k.a Diamond: là thế nước đôi hình thoi rỗng ruột suýt tạo thành dấu cộng
        //     CSBL a.k.a Cross Bottom Left: thanh đáy và thanh trái của dấu cộng giao tâm O
        //     CSBR a.k.a Cross Bottom Right: thanh đáy và thanh phải của dấu cộng giao tâm O
        //     CSTL a.k.a Cross Top Left: thanh đỉnh và thanh trái của dấu cộng giao tâm O
        //     CSTR a.k.a Cross Top Right: thanh đỉnh và thanh phải của dấu cộng giao tâm O
        //     SS_SS a.k.a Cross: là thế nước đôi tạo thành nón lá ko chóp
        //     SS_SSD a.k.a Cross Down: là thế nước đôi tạo thành nón lá chổng ngược ko chóp
        //     SS_SSL a.k.a Cross Left: là thế nước đôi tạo thành nón lá ko chóp xoay trái 90 độ
        //     SS_SSR a.k.a Cross Right: là thế nước đôi tạo thành nón lá ko chóp xoay phải 90 độ
        //     RTBL a.k.a Right Triangle Bottom Left: là tam giác vuông cân không có cạnh đáy với góc vuông ở phía Bottom Left với đỉnh Top Left là O
        //     RTBR a.k.a Right Triangle Bottom Right: là tam giác vuông cân không có cạnh đáy với góc vuông ở phía Bottom Right với đỉnh Top Right là O
        //     RTTL a.k.a Right Triangle Top Left: là tam giác vuông cân không có cạnh đỉnh với góc vuông ở phía Top Left với đỉnh Bottom Left là O
        //     RTTR a.k.a Right Triangle Top Right: là tam giác vuông cân không có cạnh đỉnh với góc vuông ở phía Top Right với đỉnh Bottom Right là O
        //     BTBL a.k.a Bottom Triangle Bottom Left: là tam giác vuông cân không có cạnh kề với góc vuông ở phía Bottom Left với đỉnh Top Left là O
        //     BTBR a.k.a Bottom Triangle Bottom Right: là tam giác vuông cân không có cạnh kề với góc vuông ở phía Bottom Right với đỉnh Top Right là O
        //     BTTL a.k.a Bottom Triangle Top Left: là tam giác vuông cân không có cạnh kề với góc vuông ở phía Top Left với đỉnh Bottom Left là O
        //     BTTR a.k.a Bottom Triangle Top Right: là tam giác vuông cân không có cạnh kề với góc vuông ở phía Top Right với đỉnh Bottom Right là O
        //     135DATL a.k.a 135 Degree Angle Top Left: góc 135 độ ở vị trí top left
        //     135DATR a.k.a 135 Degree Angle Top Left: góc 135 độ ở vị trí top left
        //     135DABL a.k.a 135 Degree Angle Top Left: góc 135 độ ở vị trí top left
        //     135DABR a.k.a 135 Degree Angle Top Left: góc 135 độ ở vị trí top left
        // 6, Kiến tạo cho đủ 4O
        // 7, Kiến tạo cho đủ 3O
        String[] priorityPatterns = {
                "XX_XX", "X_XX", "XX_X", // 1
                "O_O_O", "_OO_O_", "_O_OO_", // 2
                "__XXX__", "X_X_X", "_XXX_", "OXX_X_", "_X_XXO", "OX_XX_", "_XX_XO",  // 3
                "SWES", "DMND", "135DATL", "135DATR", "135DABL", "135DABR", "CSBL", "CSBR", "CSTL", "CSTR", "BTBL", "BTBR", "BTTL", "BTTR", "SS_SS", "SS_SSD", "SS_SSL", "SS_SSR", "RTBL", "RTBR", "RTTL", "RTTR", // 4
                "__OOO__", "OO__O", "O__OO",  "_OOO_",  // 5
                "SWESO", "DMNDO", "135DATLO", "135DATRO", "135DABLO", "135DABRO", "CSBLO", "CSBRO", "CSTLO", "CSTRO", "BTBLO", "BTBRO", "BTTLO", "BTTRO", "SS_SSO", "SS_SSDO", "SS_SSLO", "SS_SSRO", "RTBLO", "RTBRO", "RTTLO", "RTTRO", // 4
                "XOOO__", "__OOOX", "XOO_O__", "__O_OOX", "XOOO_N", "N_OOOX", // 6
                "__O_O__", "___OO___", "__OO__", "XOO__", "__OOX",  "_O_O_", "_OO_" // 7
        };

        // Duyệt từng mẫu theo thứ tự ưu tiên
        for (String pattern : priorityPatterns) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    for (int[] dir : directions) {
                        int dx = dir[0], dy = dir[1];
                        if (blockPattern(i, j, dx, dy, pattern)) {

                            // Phát âm thanh khi đánh
                            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sound);
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(mp -> {
                                mp.release();
                            }); // Giải phóng sau khi phát âm thanh, tránh rò rỉ bộ nhớ

                            return true;
                        }
                    }
                }
            }
        }

        return false; // Không tìm thấy nước đi phù hợp
    }


    // Hàm kiểm tra và đặt "O" vào vị trí thích hợp theo từng mẫu
    private boolean blockPattern(int x, int y, int dx, int dy, String pattern) {
        switch (pattern) {
            case "_XXX_":
                if (isEmpty(x, y) &&
                        isX(x + dx, y + dy) &&
                        isX(x + 2 * dx, y + 2 * dy) &&
                        isX(x + 3 * dx, y + 3 * dy) &&
                        isEmpty(x + 4 * dx, y + 4 * dy)
                ) {
                    return block(x, y) || block(x + 4 * dx, y + 4 * dy);
                }
                break;

            case "__XXX__":
                if (isEmpty(x - dx, y - dy) &&
                        isEmpty(x, y) &&
                        isX(x + dx, y + dy) &&
                        isX(x + 2 * dx, y + 2 * dy) &&
                        isX(x + 3 * dx, y + 3 * dy) &&
                        isEmpty(x + 4 * dx, y + 4 * dy) &&
                        isEmpty(x + 5 * dx, y + 5 * dy)
                ) {
                    return block(x, y) || block(x + 4 * dx, y + 4 * dy);
                }
                break;

            case "_OOO_":
                if (isEmpty(x, y) &&
                        isO(x + dx, y + dy) &&
                        isO(x + 2 * dx, y + 2 * dy) &&
                        isO(x + 3 * dx, y + 3 * dy) &&
                        isEmpty(x + 4 * dx, y + 4 * dy)) {
                    return block(x, y) || block(x + 4 * dx, y + 4 * dy);
                }
                break;

            case "__OOO__":
                if (isEmpty(x, y) &&
                        isEmpty(x + dx, y + dy) &&
                        isO(x + 2 * dx, y + 2 * dy) &&
                        isO(x + 3 * dx, y + 3 * dy) &&
                        isO(x + 4 * dx, y + 4 * dy) &&
                        isEmpty(x + 5 * dx, y + 5 * dy) &&
                        isEmpty(x + 6 * dx, y + 6 * dy)) {
                    return block(x + dx, y + dy) || block(x + 5 * dx, y + 5 * dy);
                }

            case "_O_O_":
                if (isEmpty(x, y) &&
                        isO(x + dx, y + dy) &&
                        isEmpty(x + 2 * dx, y + 2 * dy) &&
                        isO(x + 3 * dx, y + 3 * dy) &&
                        isEmpty(x + 4 * dx, y + 4 * dy)) {
                    return block(x + 2 * dx, y + 2*dy) || block(x, y) || block(x + 4 * dx, y + 4*dy);
                }
                break;

            case "__O_O__":
                if (isEmpty(x - dx, y - dy) &&
                        isEmpty(x, y) &&
                        isO(x + dx, y + dy) &&
                        isEmpty(x + 2 * dx, y + 2 * dy) &&
                        isO(x + 3 * dx, y + 3 * dy) &&
                        isEmpty(x + 4 * dx, y + 4 * dy) &&
                        isEmpty(x + 5 * dx, y + 5 * dy)) {
                    return block(x + 2 * dx, y + 2*dy) || block(x, y) || block(x + 4 * dx, y + 4*dy);
                }
                break;

            case "O_O_O":
                if (
                        isO(x, y) &&
                                isEmpty(x + dx, y + dy) &&
                                isO(x + 2 * dx, y + 2 * dy) &&
                                isEmpty(x + 3 * dx, y + 3 * dy) &&
                                isO(x + 4 * dx, y + 4 * dy)
                ) {
                    return block(x + dx, y + dy) || block(x + 3 * dx, y + 3 * dy);
                }
                break;

            case "X_XX":
                if (isX(x, y) &&
                        isEmpty(x + dx, y + dy) &&
                        isX(x + 2 * dx, y + 2 * dy) &&
                        isX(x + 3 * dx, y + 3 * dy)) {
                    return block(x + dx, y + dy);
                }
                break;

            case "XX_X":
                if (isX(x, y) &&
                        isX(x + dx, y + dy) &&
                        isEmpty(x + 2 * dx, y + 2 * dy) &&
                        isX(x + 3 * dx, y + 3 * dy)) {
                    return block(x + 2 * dx, y + 2 * dy);
                }
                break;

            case "XX_XX":
                if (isX(x, y) &&
                        isX(x + dx, y + dy) &&
                        isEmpty(x + 2 * dx, y + 2 * dy) &&
                        isX(x + 3 * dx, y + 3 * dy) &&
                        isX(x + 4 * dx, y + 4 * dy)) {
                    return block(x + 2 * dx, y + 2 * dy);
                }
                break;

            case "X_X_X":
                if (isX(x, y) &&
                        isEmpty(x + dx, y + dy) &&
                        isX(x + 2 * dx, y + 2 * dy) &&
                        isEmpty(x + 3 * dx, y + 3 * dy) &&
                        isX(x + 4 * dx, y + 4 * dy)) {
                    return block(x + dx, y + dy) || block(x + 3 * dx, y + 3 * dy);
                }
                break;

            case "__O_OOX":
                if (isEmpty(x - dx, y - dy) &&
                        isEmpty(x, y)  // Dấu "_" đầu tiên
                        && isO(x + dx, y + dy)  // "O"
                        && isEmpty(x + 2 * dx, y + 2 * dy)  // Dấu "_" thứ hai
                        && isO(x + 3 * dx, y + 3 * dy)  // "O"
                        && isO(x + 4 * dx, y + 4 * dy)  // "O"
                        && isX(x + 5 * dx, y + 5 * dy)) { // "X" ở cuối
                    return block(x, y) || block(x + 2 * dx, y + 2 * dy); // Chặn tại "_" đầu hoặc thứ hai
                }
                break;

            case "OXX_X_":
                if (
                        isO(x, y) &&
                                isX(x + dx, y + dy) &&
                                isX(x + 2 * dx, y + 2 * dy) &&
                                isEmpty(x + 3 * dx, y + 3 * dy) &&
                                isX(x + 4 * dx, y + 4 * dy) &&
                                isEmpty(x + 5 * dx, y + 5 * dy)
                ) {
                    return block(x + 3 * dx, y + 3 * dy) || block(x + 5 * dx, y + 5 * dy);
                }
                break;

            case "_X_XXO":
                if (isEmpty(x, y)   // "_"
                        && isX(x + dx, y + dy) // "X"
                        && isEmpty(x + 2 * dx, y + 2 * dy) // "_"
                        && isX(x + 3 * dx, y + 3 * dy) // "X"
                        && isO(x + 4 * dx, y + 4 * dy)) { // "O"
                    return block(x, y) || block(x + 2 * dx, y + 2 * dy);
                }
                break;

            case "OX_XX_":
                if (isO(x, y)   // "O"
                        && isX(x + dx, y + dy) // "X"
                        && isEmpty(x + 2 * dx, y + 2 * dy) // "_"
                        && isX(x+ 3 * dx, y + 3 * dy) // "X"
                        && isX(x + 4 * dx, y + 4 * dy)
                        && isEmpty(x + 5 * dx, y + 5 * dy)) { // "_"
                    return block(x + 2 * dx, y + 2 * dy) || block(x + 5 * dx, y + 5 * dy);
                }
                break;

            case "_XX_XO":
                if (isEmpty(x, y)   // "_"
                        && isX(x + dx, y + dy) // "X"
                        && isX(x + 2 * dx, y + 2 * dy) // "X"
                        && isEmpty(x + 3 * dx, y + 3 * dy) // "_"
                        && isX(x + 4 * dx, y + 4 * dy) // "X"
                        && isO(x + 5 * dx, y + 5 *dy)) { // "O"
                    return block(x, y) || block(x + 3 * dx, y + 3 * dy);
                }
                break;

            case "_OO_O_":
                if (isEmpty(x - dx, y - dy) &&
                        isO(x, y)
                        && isO(x + dx, y + dy)
                        && isEmpty(x + 2*dx, y + 2*dy)
                        && isO(x + 3*dx, y + 3*dy)
                && isEmpty(x + 4 * dx, y + 4 * dy)) {
                    return block(x + 2*dx, y + 2*dy); // Chặn ô trống ở giữa
                }
                break;

            case "_O_OO_":
                if (isEmpty(x - dx, y - dy) &&
                        isO(x, y)
                        && isEmpty(x + dx, y + dy)
                        && isO(x + 2*dx, y + 2*dy)
                        && isO(x + 3*dx, y + 3*dy)
                        && isEmpty(x + 4 * dx, y + 4 * dy)) {
                    return block(x + dx, y + dy); // Chặn ô trống ở giữa
                }
                break;

            case "XOO_O__":
                if (isX(x, y)  // "X" đầu tiên
                        && isO(x + dx, y + dy)  // "O"
                        && isO(x + 2 * dx, y + 2 * dy)  // "O"
                        && isEmpty(x + 3 * dx, y + 3 * dy)  // Dấu "_" đầu tiên
                        && isO(x + 4 * dx, y + 4 * dy)  // "O"
                        && isEmpty(x + 5 * dx, y + 5 * dy)
                        && isEmpty(x + 6 * dx, y + 6 * dy)) {
                    return block(x + 3 * dx, y + 3 * dy) || block(x + 5 * dx, y + 5 * dy); // Chặn 1 trong 2 vị trí
                }
                break;

            case "XOOO__":
                if (isX(x, y)
                        && isO(x + dx, y + dy)
                        && isO(x + 2*dx, y + 2*dy)
                        && isO(x + 3*dx, y + 3*dy)
                        && isEmpty(x + 4*dx, y + 4*dy)
                        && isEmpty(x + 5*dx, y + 5*dy)) {
                    return block(x + 4*dx, y + 4*dy);
                }
                break;

            case "__OOOX":
                // Kiểm tra 5 ô: __OOO và X ở cuối (6 ô nhưng X là ô ngoài cùng, không cần kiểm tra)
                if (isEmpty(x, y)
                        && isEmpty(x + dx, y + dy)
                        && isO(x + 2*dx, y + 2*dy)
                        && isO(x + 3*dx, y + 3*dy)
                        && isO(x + 4*dx, y + 4*dy)
                        && isX(x + 5*dx, y + 5*dy)) {
                    return block(x + dx, y + dy);
                }
                break;

            case "_OO_":
                if (isEmpty(x, y)
                        && isO(x + dx, y + dy)
                        && isO(x + 2*dx, y + 2*dy)
                        && isEmpty(x + 3*dx, y + 3*dy)) {
                    return block(x, y) || block(x + 3*dx, y + 3*dy);
                }
                break;

            case "__OO__":
                if (isEmpty(x, y)
                        && isEmpty(x + dx, y + dy)
                        && isO(x + 2*dx, y + 2*dy)
                        && isO(x + 3*dx, y + 3*dy)
                        && isEmpty(x + 4*dx, y + 4*dy)
                        && isEmpty(x + 5*dx, y + 5*dy)) {
                    return block(x + dx, y + dy) || block(x + 4*dx, y + 4*dy);
                }
                break;

            case "___OO___":
                if (
                        isEmpty(x, y) &&
                                isEmpty(x + dx, y + dy) &&
                                isEmpty(x + 2 * dx, y + 2 * dy) &&
                                isO(x + 3 * dx, y + 3 * dy) &&
                                isO(x + 4 * dx, y + 4 * dy) &&
                                isEmpty(x + 5 * dx, y + 5 * dy) &&
                                isEmpty(x + 6 * dx, y + 6 * dy) &&
                                isEmpty(x + 7 * dx, y + 7 * dy)
                ) {
                    return block(x + dx, y + dy) || block(x + 6 * dx, y + 6 * dy);
                }
                break;

            case "XOO__":
                if (isX(x, y)
                        && isO(x + dx, y + dy)
                        && isO(x + 2*dx, y + 2*dy)
                        && isEmpty(x + 3*dx, y + 3*dy)
                        && isEmpty(x + 4*dx, y + 4*dy)) {
                    return block(x + 3*dx, y + 3*dy) || block(x + 4*dx, y + 4*dy);
                }
                break;

            case "__OOX":
                if (isEmpty(x, y)
                        && isEmpty(x + dx, y + dy)
                        && isO(x + 2*dx, y + 2*dy)
                        && isO(x + 3*dx, y + 3*dy)
                        && isX(x + 4*dx, y + 4*dy)) {
                    return block(x + dx, y + dy) || block(x, y);
                }
                break;

            case "O__OO":  // Thêm xử lý cho O__OO
                if (isO(x, y) &&
                        isEmpty(x + dx, y + dy) &&
                        isEmpty(x + 2 * dx, y + 2 * dy) &&
                        isO(x + 3 * dx, y + 3 * dy) &&
                        isO(x + 4 * dx, y + 4 * dy)) {
                    return block(x + dx, y + dy) || block(x + 2 * dx, y + 2 * dy);
                }
                break;

            case "OO__O":  // Thêm xử lý cho OO__O
                if (isO(x, y) &&
                        isO(x + dx, y + dy) &&
                        isEmpty(x + 2 * dx, y + 2 * dy) &&
                        isEmpty(x + 3 * dx, y + 3 * dy) &&
                        isO(x + 4 * dx, y + 4 * dy)) {
                    return block(x + 2 * dx, y + 2 * dy) || block(x + 3 * dx, y + 3 * dy);
                }
                break;

            case "RTBR":
                if (
                        isEmpty(x, y) &&
                                isX(x, y + dy) &&
                                isX(x - dx, y + dy) &&
                                isX(x, y + 2 * dy) &&
                                isX(x - 2 * dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "RTBRO" :
                if (
                        isEmpty(x, y) &&
                                isO(x, y + dy) &&
                                isO(x - dx, y + dy) &&
                                isO(x, y + 2 * dy) &&
                                isO(x - 2 * dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "RTBL":
                if (
                        isEmpty(x, y) &&
                                isX(x, y + dy) &&
                                isX(x + dx, y + dy) &&
                                isX(x, y + 2 * dy) &&
                                isX(x + 2 * dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "RTBLO":
                if (
                        isEmpty(x, y) &&
                                isO(x, y + dy) &&
                                isO(x + dx, y + dy) &&
                                isO(x, y + 2 * dy) &&
                                isO(x + 2 * dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "RTTL":
                if (
                        isEmpty(x, y) && // Ô cần đặt phải trống
                                isX(x, y + dy) &&
                                isX(x, y + 2 * dy) &&
                                isX(x + 2 * dx, y + dy) &&
                                isX(x + dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "RTTLO":
                if (
                        isEmpty(x, y) && // Ô cần đặt phải trống
                                isO(x, y + dy) &&
                                isO(x, y + 2 * dy) &&
                                isO(x + 2 * dx, y + dy) &&
                                isO(x + dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "RTTR":
                if (
                    // X_X
                        isX(x, y - 2 * dy) &&
                                isX(x - dx, y - 2 * dy) &&
                                // _XX
                                isX(x, y - dy) &&
                                isX(x - 2 * dx, y - dy) &&
                                // ____
                                isEmpty(x, y)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "RTTRO":
                if (
                        // O_O
                        isO(x, y - 2 * dy) &&
                                isO(x - dx, y - 2 * dy) &&
                                // _OO
                                isO(x, y - dy) &&
                                isO(x - 2 * dx, y - dy) &&
                                // ___
                                isEmpty(x, y)
                ) {
                    return block(x, y); // Đặt "O" vào vị trí chặn
                }
                break;

            case "BTBL":
                if (isEmpty(x, y) &&
                isX(x - dx, y) &&
                isX(x - dx, y - dy) &&
                isX(x - 2 * dx, y) &&
                isX(x - 2 * dx, y - 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "BTBLO" :
                if (isEmpty(x, y) &&
                        isO(x - dx, y) &&
                        isO(x - dx, y - dy) &&
                        isO(x - 2 * dx, y) &&
                        isO(x - 2 * dx, y - 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "BTBR":
                if (isEmpty(x, y) &&
                isX(x + dx, y) &&
                isX(x + dx, y - dy) &&
                isX(x + 2 * dx, y) &&
                isX(x + 2 * dx, y - 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "BTBRO":
                if (isEmpty(x, y) &&
                        isO(x + dx, y) &&
                        isO(x + dx, y - dy) &&
                        isO(x + 2 * dx, y) &&
                        isO(x + 2 * dx, y - 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "BTTL":
                if (isX(x, y) &&
                isX(x + dx, y) &&
                isEmpty(x + 2 * dx, y) &&
                isX(x + dx, y + dy) &&
                isX(x, y + 2 * dy)) {
                    return block(x + 2 * dx, y);
                }
                break;

            case "BTTLO":
                if (isO(x, y) &&
                        isO(x + dx, y) &&
                        isEmpty(x + 2 * dx, y) &&
                        isO(x + dx, y + dy) &&
                        isO(x, y + 2 * dy)) {
                    return block(x + 2 * dx, y);
                }
                break;

            case "BTTR":
                if (isEmpty(x, y) &&
                isX(x + dx, y) &&
                isX(x + 2 * dx, y) &&
                isX(x + dx, y + dy) &&
                isX(x + 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "BTTRO":
                if (isEmpty(x, y) &&
                        isO(x + dx, y) &&
                        isO(x + 2 * dx, y) &&
                        isO(x + dx, y + dy) &&
                        isO(x + 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "XOOO_N":
                if (isX(x, y)  // "N" ngoài bàn cờ
                        && isO(x + dx, y + dy)  // "O"
                        && isO(x + 2 * dx, y + 2 * dy)  // "O"
                        && isO(x + 3 * dx, y + 3 * dy)
                        && isEmpty(x + 4 * dx, y + 4 * dy)// "O"
                        && isOutOfBounds(x + 5 * dx, y + 5 * dy)) { // "X"
                    return false; // Không đánh "O"
                }
                break;

            case "N_OOOX":
                if (isOutOfBounds(x - dx, y - dy)  // "N" ngoài bàn cờ
                        && isEmpty(x, y)  // Dấu "_"
                        && isO(x + dx, y + dy)  // "O"
                        && isO(x + 2 * dx, y + 2 * dy)  // "O"
                        && isO(x + 3 * dx, y + 3 * dy)  // "O"
                        && isX(x + 4 * dx, y + 4 * dy)) { // "X"
                    return false; // Không đánh "O"
                }
                break;

            case "SS_SS":
                if (isEmpty(x, y) &&
                        isX(x - dx, y + dy) &&
                        isX(x + dx, y + dy) &&
                        isX(x - 2 * dx, y + 2 * dy) &&
                        isX(x + 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "SS_SSO":
                if (isEmpty(x, y) &&
                isO(x - dx, y + dy) &&
                isO(x + dx, y + dy) &&
                isO(x - 2 * dx, y + 2 * dy) &&
                isO(x + 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "SS_SSD":
                if (isX(x - 2 * dx, y - 2 * dy) &&
                        isX(x - dx, y - dy) &&
                        isEmpty(x, y) &&
                        isX(x + dx, y - dy) &&
                        isX(x + 2 * dx, y - 2 * dy)
                ) {
                    return block(x, y);
                }
            break;

            case "SS_SSDO":
                if (isO(x - 2 * dx, y - 2 * dy) &&
                        isO(x - dx, y - dy) &&
                        isEmpty(x, y) &&
                        isO(x + dx, y - dy) &&
                        isO(x + 2 * dx, y - 2 * dy)
                ) {
                    return block(x, y);
                }
                break;

            case "SS_SSL":
                if (isX(x + 2 * dx, y - 2 * dy) &&
                        isX(x + dx, y - dy) &&
                        isEmpty(x, y) &&
                        isX(x + dx, y + dy) &&
                        isX(x + 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "SS_SSLO":
                if (isO(x + 2 * dx, y - 2 * dy) &&
                        isO(x + dx, y - dy) &&
                        isEmpty(x, y) &&
                        isO(x + dx, y + dy) &&
                        isO(x + 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;


            case "SS_SSR":
                if (isX(x -2 * dx, y - 2 * dy) &&
                        isX(x - dx, y - dy) &&
                        isEmpty(x, y) &&
                        isX(x - dx, y + dy) &&
                        isX(x - 2 * dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào chóp
                }
                break;

            case "SS_SSRO":
                if (isO(x -2 * dx, y - 2 * dy) &&
                        isO(x - dx, y - dy) &&
                        isEmpty(x, y) &&
                        isO(x - dx, y + dy) &&
                        isO(x - 2 * dx, y + 2 * dy)
                ) {
                    return block(x, y); // Đặt "O" vào chóp
                }
                break;

            case "DMND":
                if (
                    // Kiểm tra ô trung tâm trống
                        isEmpty(x, y) &&
                                isX(x - dx, y) && isX(x + dx, y) && // Hai quân X nằm ngang
                                isX(x, y - dy) && isX(x, y + dy) // Hai quân X nằm dọc
                ) {
                    return block(x, y); // Đặt "O" vào trung tâm
                }
                break;

            case "DMNDO":
                if (
                    // Kiểm tra ô trung tâm trống
                        isEmpty(x, y) &&

                                // Kiểm tra các dấu "X" tạo thành hình thoi
                                isO(x - dx, y) && isO(x + dx, y) && // Hai quân X nằm ngang
                                isO(x, y - dy) && isO(x, y + dy) // Hai quân X nằm dọc
                ) {
                    return block(x, y); // Đặt "O" vào trung tâm
                }
                break;

            case "135DATL":
                if (isEmpty(x, y) &&
                isX(x + dx, y) &&
                isX(x + 2 * dx, y) &&
                isX(x - dx, y + dy) &&
                isX(x - 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "135DATLO":
                if (isEmpty(x, y) &&
                        isO(x + dx, y) &&
                        isO(x + 2 * dx, y) &&
                        isO(x - dx, y + dy) &&
                        isO(x - 2 * dx, y + 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "135DATR":
                if (isX(x, y) &&
                isX(x + dx, y) &&
                isEmpty(x + 2 * dx, y) &&
                isX(x + dx, y + dy) &&
                isX(x + 2 * dx, y + 2 * dy)) {
                    return block(x + 2 * dx, y);
                }
                break;

            case "135DATRO":
                if (isO(x, y) &&
                        isO(x + dx, y) &&
                        isEmpty(x + 2 * dx, y) &&
                        isO(x + dx, y + dy) &&
                        isO(x + 2 * dx, y + 2 * dy)) {
                    return block(x + 2 * dx, y);
                }
                break;

            case "135DABL":
                if (isX(x, y) &&
                isX(x + dx, y + dy) &&
                isEmpty(x + 2 * dx, y + 2 * dy) &&
                isX(x + 3 * dx, y + 2 * dy) &&
                        isX(x + 4 * dx, y + 2 * dy)) {
                    return block(x + 2 * dx, y + 2 * dy);
                }
                break;

            case "135DABLO":
                if (isO(x, y) &&
                        isO(x + dx, y + dy) &&
                        isEmpty(x + 2 * dx, y + 2 * dy) &&
                        isO(x + 3 * dx, y + 2 * dy) &&
                        isO(x + 4 * dx, y + 2 * dy)) {
                    return block(x + 2 * dx, y + 2 * dy);
                }
                break;

            case "135DABR":
                if (isX(x, y) &&
                isX(x + dy, y) &&
                isEmpty(x + 2 * dx, y) &&
                isX(x + 3 * dx, y - dy) &&
                isX(x + 4 * dx, y - 2 * dy)) {
                    return block(x + 2 * dx, y);
                }
                break;

            case "135DABRO":
                if (isO(x, y) &&
                        isO(x + dy, y) &&
                        isEmpty(x + 2 * dx, y) &&
                        isO(x + 3 * dx, y - dy) &&
                        isO(x + 4 * dx, y - 2 * dy)) {
                    return block(x + 2 * dx, y);
                }
                break;

            case "SWES":
                if (
                    // Kiểm tra ô trung tâm trống
                        isEmpty(x, y) &&
                                isX(x - dx, y - dy) && isX(x + dx, y - dy) && // Hai góc trên
                                isX(x - dx, y + dy) && isX(x + dx, y + dy) // Hai góc dưới
                ) {
                    return block(x, y); // Đặt "O" vào giữa
                }
                break;

            case "SWESO":
                if (
                    // Kiểm tra ô trung tâm trống
                        isEmpty(x, y) &&
                                isO(x - dx, y - dy) && isO(x + dx, y - dy) && // Hai góc trên
                                isO(x - dx, y + dy) && isO(x + dx, y + dy) // Hai góc dưới
                ) {
                    return block(x, y); // Đặt "O" vào giữa
                }
                break;

            case "CSBL":
                if (isEmpty(x, y)
                        // XX_
                        && isX(x - dx, y)
                        && isX(x - 2 * dx, y)
                        // __X
                        // __X
                        && isX(x, y + dy)
                        && isX(x, y + 2 * dy)
                ) {
                    return block(x, y);
                }
                break;

            case "CSBLO":
                if (isEmpty(x, y)
                        // OO_
                        && isO(x - dx, y)
                        && isO(x - 2 * dx, y)
                        // __O
                        // __O
                        && isO(x, y + dy)
                        && isO(x, y + 2 * dy)
                ) {
                    return block(x, y);
                }
                break;

            case "CSBR":
                if (isEmpty(x, y)
                        // _XX
                        && isX(x + dx, y)
                        && isX(x + 2 * dx, y)
                        // X__
                        // X__
                        && isX(x, y + dy)
                        && isX(x, y + 2 * dy)
                ) {
                    return block(x, y);
                }
                break;

            case "CSBRO":
                if (isEmpty(x, y)
                        // _OO
                        && isO(x + dx, y)
                        && isO(x + 2 * dx, y)
                        // O__
                        // O__
                        && isO(x, y + dy)
                        && isO(x, y + 2 * dy)
                ) {
                    return block(x, y);
                }
                break;

            case "CSTL":
                if (isEmpty(x, y)
                        && isX(x - 2 * dx, y)
                        && isX(x - dx, y)
                        && isX(x, y - dy)
                        && isX(x, y - 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "CSTLO":
                if (isEmpty(x, y)
                        && isO(x - 2 * dx, y)
                        && isO(x - dx, y)
                        && isO(x, y - dy)
                        && isO(x, y - 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "CSTR":
                if (isEmpty(x, y)
                        && isX(x + 2 * dx, y)
                        && isX(x + dx, y)
                        && isX(x, y - dy)
                        && isX(x, y - 2 * dy)) {
                    return block(x, y);
                }
                break;

            case "CSTRO":
                if (isEmpty(x, y)
                        && isO(x + 2 * dx, y)
                        && isO(x + dx, y)
                        && isO(x, y - dy)
                        && isO(x, y - 2 * dy)) {
                    return block(x, y);
                }
                break;
        }
        return false;
    }

    // Đặt "O" vào vị trí chặn
    private boolean block(int x, int y) {
        if (isValidMove(x, y)) {
            buttons[x][y].setText("O");
            blockedPositions.add(new Point(x, y));
            return true;
        }
        return false;
    }

    // Kiểm tra "X", "O" hoặc ô trống
    private boolean isX(int x, int y) {
        return isValidCoord(x, y) && buttons[x][y].getText().equals("X");
    }

    private boolean isO(int x, int y) {
        return isValidCoord(x, y) && buttons[x][y].getText().equals("O");
    }

    private boolean isEmpty(int x, int y) {
        return isValidCoord(x, y) && buttons[x][y].getText().equals("");
    }

    private boolean isValidMove(int x, int y) {
        return isEmpty(x, y);
    }

    private boolean isValidCoord(int x, int y) {
        return x >= 0 && x < rows && y >= 0 && y < columns;
    }

    private boolean isOutOfBounds(int x, int y) {
        return x < 0 || x >= rows || y < 0 || y >= columns;
    }

    private int[] findStrategicMove() {
        // Ưu tiên đánh vào trung tâm
        int centerRow = rows / 2;
        int centerCol = columns / 2;
        if (buttons[centerRow][centerCol].getText().toString().equals("")) {
            return new int[]{centerRow, centerCol};
        }

        // Ưu tiên đánh vào các góc
        int[][] corners = {
                {0, 0},
                {0, columns - 1},
                {rows - 1, 0},
                {rows - 1, columns - 1}
        };
        for (int[] corner : corners) {
            int i = corner[0];
            int j = corner[1];
            if (buttons[i][j].getText().toString().equals("")) {
                return new int[]{i, j};
            }
        }

        // Nếu không có vị trí chiến lược, trả về null
        return null;
    }

    private boolean findWinningMove(String symbol) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (buttons[i][j].getText().toString().equals("")) {
                    buttons[i][j].setText(symbol);
                    boolean win = checkWinFor(symbol);
                    if (win) {
                        playerX = true; // Chuyển lượt sau khi bot đánh xong
                        return true;
                    }
                    buttons[i][j].setText(""); // Nếu không thắng, hoàn trả
                }
            }
        }
        return false;
    }

    private boolean findBlockingMove(String checkSymbol, String placeSymbol) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (buttons[i][j].getText().toString().equals("")) {
                    buttons[i][j].setText(checkSymbol);
                    boolean win = checkWinFor(checkSymbol);
                    buttons[i][j].setText(""); // Hoàn trả ô về trống

                    if (win) {
                        // Phát âm thanh khi đánh
                        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sound);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(mp -> {
                            mp.release();
                        }); // Giải phóng sau khi phát âm thanh, tránh rò rỉ bộ nhớ

                        buttons[i][j].setText(placeSymbol); // Bot đặt "O" thay vì "X"
                        return true; // Không kiểm tra thắng ngay, vì đây là nước chặn
                    }
                }
            }
        }
        return false;
    }

    private boolean checkWinFor(String player) {
        boolean hasWon = false;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (buttons[i][j].getText().toString().equals(player)) {
                    if (checkLine(i, j, 0, 1, player) || // Kiểm tra hàng ngang
                            checkLine(i, j, 1, 0, player) || // Kiểm tra hàng dọc
                            checkLine(i, j, 1, 1, player) || // Kiểm tra chéo xuôi
                            checkLine(i, j, 1, -1, player))  // Kiểm tra chéo ngược
                    {
                        hasWon = true;
                    }
                }
            }
        }
        return hasWon;
    }

    private void highlightWinningButtons() {
        int softYellow = Color.parseColor("#FFF9C4"); // Màu vàng nhạt dịu mắt
        for (int[] pos : winningPositions) {
            buttons[pos[0]][pos[1]].setBackgroundTintList(ColorStateList.valueOf(softYellow));
        }
    }


    private void saveWinningPositions(int row, int col, int rowStep, int colStep) {
        winningPositions.clear();
        for (int k = 0; k < winCondition; k++) {
            int newRow = row + k * rowStep;
            int newCol = col + k * colStep;
            winningPositions.add(new int[]{newRow, newCol});
        }
    }

    private boolean checkWin() {
        for (String player : new String[]{"X", "O"}) {
            winningPositions.clear(); // Xóa danh sách vị trí trước khi kiểm tra

            // Kiểm tra hàng ngang
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j <= columns - winCondition; j++) {
                    if (checkLine(i, j, 0, 1, player)) {
                        saveWinningPositions(i, j, 0, 1);
                        highlightWinningButtons();
                        showResultDialog(player + " thắng!");
                        return true;
                    }
                }
            }
            // Kiểm tra hàng dọc
            for (int i = 0; i < columns; i++) {
                for (int j = 0; j <= rows - winCondition; j++) {
                    if (checkLine(j, i, 1, 0, player)) {
                        saveWinningPositions(j, i, 1, 0);
                        highlightWinningButtons();
                        showResultDialog(player + " thắng!");
                        return true;
                    }
                }
            }
            // Kiểm tra đường chéo xuôi
            for (int i = 0; i <= rows - winCondition; i++) {
                for (int j = 0; j <= columns - winCondition; j++) {
                    if (checkLine(i, j, 1, 1, player)) {
                        saveWinningPositions(i, j, 1, 1);
                        highlightWinningButtons();
                        showResultDialog(player + " thắng!");
                        return true;
                    }
                }
            }
            // Kiểm tra đường chéo ngược
            for (int i = 0; i <= rows - winCondition; i++) {
                for (int j = winCondition - 1; j < columns; j++) {
                    if (checkLine(i, j, 1, -1, player)) {
                        saveWinningPositions(i, j, 1, -1);
                        highlightWinningButtons();
                        showResultDialog(player + " thắng!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Kiểm tra các dòng, cột và đường chéo
    private boolean checkLine(int row, int col, int rowStep, int colStep, String player) {
        for (int k = 0; k < winCondition; k++) {
            int newRow = row + k * rowStep;
            int newCol = col + k * colStep;
            if (newRow < 0 || newRow >= rows || newCol < 0 || newCol >= columns) {
                return false; // Tránh vượt ra ngoài biên
            }
            if (!buttons[newRow][newCol].getText().toString().equals(player)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkDraw() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (buttons[i][j].getText().toString().equals("")) {
                    return false; // Vẫn còn ô trống, chưa hòa
                }
            }
        }
        showResultDialog("Hòa rồi!");
        return true;
    }

    private void showResultDialog(String message) {
        gameOver = true;
        SpannableString spannable = new SpannableString("Kết quả: " + message);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Toast.makeText(this, spannable, Toast.LENGTH_SHORT).show();
    }
}

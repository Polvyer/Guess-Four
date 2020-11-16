package com.example.williamocampoproject4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // UI Handler and Worker Threads (x2)
    public UIHandler uiHandler;
    public WorkerThread1 workerThread1;
    public WorkerThread2 workerThread2;

    /*------------UI Variables------------*/
    // References to the ListView's
    public ListView listView1;
    public ListView listView2;

    // References to the TextView's
    public TextView player1;
    public TextView player2;
    public TextView secretNumber1;
    public TextView secretNumber2;

    // Used with ListView
    public ArrayList<String> arrayList1;
    public ArrayList<String> arrayList2;

    // Used with ListView
    public ArrayAdapter arrayAdapter1;
    public ArrayAdapter arrayAdapter2;

    // Stores secret numbers
    public int number1;
    public int number2;

    // Stores secret number guesses
    public int guess1;
    public int guess2;

    // Stores guess evaluations
    Map<String, Integer> thread1Evaluation;
    Map<String, Integer> thread2Evaluation;

    // Checks if game is ongoing
    boolean ongoing = false;

    // Kill switch
    boolean kill = false;

    /*------------Game Variables------------*/
    // Stores secret numbers (in array format)
    public int[] numberArray1 = new int[4];
    public int[] numberArray2 = new int[4];

    // Stores guessed numbers (in array format)
    public int[] guessArray1 = new int[4];
    public int[] guessArray2 = new int[4];

    // Depicts who's turn it is
    public boolean worker1Turn;

    // Depicts the turn number
    public int turn;

    // Digits to exclude from guess
    ArrayList<Integer> excludeDigits1 = new ArrayList<Integer>();
    ArrayList<Integer> excludeDigits2 = new ArrayList<Integer>();

    /*------------Messages------------*/
    public static final int CREATE_SECRET_NUMBER = 0; // Set up a secret sequence
    public static final int SHOW_NUMBERS = 1;         // Show the two initial numbers chosen by the worker threads
    public static final int FIRST_GUESS = 2;          // Guess the sequence of digits of their opponent
    public static final int DISPLAY_EVALUATIONS = 3;  // Display the guesses and responses of each worker thread
    public static final int EVALUATE_GUESS = 4;       // Specify the number of digits that were successfully guessed in the correct position
                                                      // and the number of digits that were successfully guessed but in the wrong position.
                                                      // In addition, the opponent would be told one of the missed digits
    public static final int GUESS = 5;                // Every other guess (more educated)

    public class UIHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case SHOW_NUMBERS: // Show the two initial numbers chosen by the worker threads
                    if (worker1Turn) {
                        // Show worker thread 1's initial number
                        secretNumber1.setText(Integer.toString(number1));

                        if (kill) {
                            kill = false;
                            return;
                        } else {
                            // Communicate to worker thread 2
                            Message m = workerThread2.handler2.obtainMessage(CREATE_SECRET_NUMBER);
                            workerThread2.handler2.sendMessage(m);
                        }
                    } else {
                        // Show worker thread 2's initial number
                        secretNumber2.setText(Integer.toString(number2));

                        if (kill) {
                            kill = false;
                            return;
                        } else {
                            // Communicate to worker thread 1
                            Message m = workerThread1.handler1.obtainMessage(FIRST_GUESS);
                            workerThread1.handler1.sendMessage(m);
                        }
                    }

                    worker1Turn = !worker1Turn; // Switch turns
                    break;

                case DISPLAY_EVALUATIONS:
                    if (worker1Turn) {
                        // Add to 1st ListView
                        arrayList1.add("Guess " + Integer.toString(turn) + ": " + Integer.toString(guess1));
                        arrayList1.add("CorrectPos: " + thread1Evaluation.get("correctPos") + " WrongPos: " + thread1Evaluation.get("wrongPos") + " BadGuess: " + thread1Evaluation.get("badGuess"));
                        listView1.setAdapter(arrayAdapter1);

                        // Add to excludes (if badGuess != -1)
                        if (thread1Evaluation.get("badGuess") != -1) {
                            excludeDigits1.add(thread1Evaluation.get("badGuess"));
                        }

                        // Check if worker thread 1 won
                        if (thread1Evaluation.get("correctPos") == 4) {
                            player1.setText("Worker Thread 1 Wins!");
                            player2.setText("Worker Thread 2 Loses!");
                            Toast.makeText(MainActivity.this, "Worker Thread 1 Wins!", Toast.LENGTH_SHORT).show();
                            ongoing = false;
                        } else {
                            if (kill) {
                                kill = false;
                                return;
                            } else {
                                // Communicate to worker thread 2
                                Message m = workerThread2.handler2.obtainMessage(FIRST_GUESS);
                                workerThread2.handler2.sendMessage(m);
                            }
                        }

                    } else {
                        // Add to 2nd ListView
                        arrayList2.add("Guess " + Integer.toString(turn) + ": " + Integer.toString(guess2));
                        arrayList2.add("CorrectPos: " + thread2Evaluation.get("correctPos") + " WrongPos: " + thread2Evaluation.get("wrongPos") + " BadGuess: " + thread2Evaluation.get("badGuess"));
                        listView2.setAdapter(arrayAdapter2);

                        // Add to excludes (if badGuess != -1)
                        if (thread2Evaluation.get("badGuess") != -1) {
                            excludeDigits2.add(thread2Evaluation.get("badGuess"));
                        }

                        // End of turn -> increment turn
                        turn++;

                        if (turn >= 21) {
                            if (thread2Evaluation.get("correctPos") == 4) {
                                player1.setText("Worker Thread 1 Loses!");
                                player2.setText("Worker Thread 2 Wins!");
                                Toast.makeText(MainActivity.this, "Worker Thread 2 Wins!", Toast.LENGTH_SHORT).show();
                                ongoing = false;

                            } else {
                                player1.setText("Worker Thread 1 Ties!");
                                player2.setText("Worker Thread 2 Ties!");
                                Toast.makeText(MainActivity.this, "Tie!", Toast.LENGTH_SHORT).show();
                                ongoing = false;
                            }
                        } else {
                            if (thread2Evaluation.get("correctPos") == 4) {
                                player1.setText("Worker Thread 1 Loses!");
                                player2.setText("Worker Thread 2 Wins!");
                                Toast.makeText(MainActivity.this, "Worker Thread 2 Wins!", Toast.LENGTH_SHORT).show();
                                ongoing = false;
                            } else {
                                if (kill) {
                                    kill = false;
                                    return;
                                } else {
                                    // Communicate to worker thread 1
                                    Message m = workerThread1.handler1.obtainMessage(FIRST_GUESS);
                                    workerThread1.handler1.sendMessage(m);
                                }
                            }
                        }
                    }

                    worker1Turn = !worker1Turn;
                    break;
            }
        }
    }

    public class WorkerThread1 extends Thread {
        // For its message queue
        public Handler handler1;

        @Override
        public void run() { // Do specific actions for thread
            // Sets up looper
            Looper.prepare();

            // Implement the communication between the three threads involved
            handler1 = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) { // Looper dispatches to if job is a message
                    switch(msg.what) { // Depending on msg.what, do different things
                        case CREATE_SECRET_NUMBER: // Set up a secret sequence
                            handler1.post(new Runnable() { // Adds runnable to receiver (Handler instance)
                                @Override
                                public void run() { // Looper calls if job is runnable
                                    try {
                                        Thread.sleep(1000); // Wait for a short time
                                    } catch (Exception e) {
                                        System.out.println(e);
                                    }

                                    // Create secret number
                                    number1 = getRandomNumber(excludeDigits1);

                                    // Convert secret number into an array of digits
                                    String temp = Integer.toString(number1);
                                    for (int i = 0; i < temp.length(); i++)
                                    {
                                        numberArray1[i] = temp.charAt(i) - '0';
                                    }

                                    if (kill) {
                                        kill = false;
                                        return;
                                    } else {
                                        // Communicate to UI thread
                                        Message message = uiHandler.obtainMessage(SHOW_NUMBERS);
                                        uiHandler.sendMessage(message);
                                    }
                                }
                            });
                            break;

                        case FIRST_GUESS:
                            handler1.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        System.out.println(e);
                                    }

                                    // Create secret number
                                    guess1 = getRandomNumber(excludeDigits1);

                                    // Convert secret number into an array of digits
                                    String temp = Integer.toString(guess1);
                                    for (int i = 0; i < temp.length(); i++)
                                    {
                                        guessArray1[i] = temp.charAt(i) - '0';
                                    }

                                    if (kill) {
                                        kill = false;
                                        return;
                                    } else {
                                        // Communicate to worker thread 2
                                        Message message = workerThread2.handler2.obtainMessage(EVALUATE_GUESS);
                                        workerThread2.handler2.sendMessage(message);
                                    }
                                }
                            });
                            break;

                        case EVALUATE_GUESS:
                            handler1.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        System.out.println(e);
                                    }

                                    // Evaluate worker thread 1's guess (actual - 1, guess - 2)
                                    thread2Evaluation = evaluateGuess(numberArray1, guessArray2);

                                    if (kill) {
                                        kill = false;
                                        return;
                                    } else {
                                        // Communicate to UI thread
                                        Message message = uiHandler.obtainMessage(DISPLAY_EVALUATIONS);
                                        uiHandler.sendMessage(message);
                                    }
                                }
                            });
                            break;
                    }
                }
            };

            //  Start processing jobs in the looper's queue
            Looper.loop();
        }
    }

    public class WorkerThread2 extends Thread {
        public Handler handler2;

        @Override
        public void run() {
            Looper.prepare();

            handler2 = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    switch(msg.what) {
                        case CREATE_SECRET_NUMBER:
                            handler2.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        System.out.println(e);
                                    }

                                    // Create secret number
                                    number2 = getRandomNumber(excludeDigits2);

                                    // Convert secret number into an array of digits
                                    String temp = Integer.toString(number2);
                                    for (int i = 0; i < temp.length(); i++)
                                    {
                                        numberArray2[i] = temp.charAt(i) - '0';
                                    }

                                    if (kill) {
                                        kill = false;
                                        return;
                                    } else {
                                        // Communicate to UI thread
                                        Message message = uiHandler.obtainMessage(SHOW_NUMBERS);
                                        uiHandler.sendMessage(message);
                                    }
                                }
                            });
                            break;

                        case FIRST_GUESS:
                            handler2.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        System.out.println(e);
                                    }

                                    // Create secret number
                                    guess2 = getRandomNumber(excludeDigits2);

                                    // Convert secret number into an array of digits
                                    String temp = Integer.toString(guess2);
                                    for (int i = 0; i < temp.length(); i++)
                                    {
                                        guessArray2[i] = temp.charAt(i) - '0';
                                    }

                                    if (kill) {
                                        kill = false;
                                        return;
                                    } else {
                                        // Communicate to UI thread
                                        Message message = workerThread1.handler1.obtainMessage(EVALUATE_GUESS);
                                        workerThread1.handler1.sendMessage(message);
                                    }
                                }
                            });
                            break;

                        case EVALUATE_GUESS:
                            handler2.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        System.out.println(e);
                                    }

                                    // Evaluate worker thread 1's guess (actual - 2, guess - 1)
                                    thread1Evaluation = evaluateGuess(numberArray2, guessArray1);

                                    if (kill) {
                                        kill = false;
                                        return;
                                    } else {
                                        // Communicate to UI thread
                                        Message message = uiHandler.obtainMessage(DISPLAY_EVALUATIONS);
                                        uiHandler.sendMessage(message);
                                    }
                                }
                            });
                            break;
                    }
                }
            };

            Looper.loop();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate UI Handler and two worker threads
        uiHandler = new UIHandler();
        workerThread1 = new WorkerThread1();
        workerThread2 = new WorkerThread2();

        // Start the two worker threads
        workerThread1.start();
        workerThread2.start();

        listView1 = (ListView) findViewById(R.id.ListView1);
        listView2 = (ListView) findViewById(R.id.ListView2);

        secretNumber1 = (TextView) findViewById(R.id.SecretNumber1);
        secretNumber2 = (TextView) findViewById(R.id.SecretNumber2);
        player1 = (TextView) findViewById(R.id.player1);
        player2 = (TextView) findViewById(R.id.player2);

        arrayList1 = new ArrayList<>();
        arrayList2 = new ArrayList<>();

        arrayAdapter1 = new ArrayAdapter(this, android.R.layout.simple_gallery_item , arrayList1);
        arrayAdapter2 = new ArrayAdapter(this, android.R.layout.simple_gallery_item , arrayList2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.StartNewGame:
                startGame(); // Starts game
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public int getRandomNumber(ArrayList<Integer> excludes) {
        int index; // Stores index used to retrieve random digit
        Random rand = new Random(); // Generates random numbers
        ArrayList<Integer> digits = new ArrayList<Integer>();  // Stores digits 0-9
        ArrayList<Integer> numbers = new ArrayList<Integer>(); // Stores 4 distinct digits
        boolean include; // whether to include digit in list

        // Add digits 0-9 (minus excludes)
        for (int i = 0; i <= 9; i++) {
            include = true;
            for (int j = 0; j < excludes.size(); j++) {
                if (i == excludes.get(j)) {
                    include = false;
                }
            }
            if (include) {
                digits.add(i);
            }
        }

        // Get 4 distinct digits
        for (int i = 0; i < 4; i++) {

            // Gets random distinct digit
            index = rand.nextInt(digits.size());
            numbers.add(digits.get(index));

            // Removes distinct digit to prevent from getting it again
            digits.remove(index);
        }

        // Prevents 0 from being the leading digit
        if (numbers.get(0) == 0) {
            return (numbers.get(1) * 1000 + numbers.get(0) * 100 + numbers.get(2) * 10 + numbers.get(3) * 1);
        }

        return (numbers.get(0) * 1000 + numbers.get(1) * 100 + numbers.get(2) * 10 + numbers.get(3) * 1);
    }

    public void startGame() {
        // Stop game (if ongoing)
        stopGame();

        ongoing = true;

        while (kill) {
            System.out.println("Waiting for threads to die");
        }

        // Prep game
        prepGame();

        // Returns message instance from global pool and sets target handler to aHandler
        Message m = workerThread1.handler1.obtainMessage(CREATE_SECRET_NUMBER);

        // Adds a message to receiver (a Handler instance)
        workerThread1.handler1.sendMessage(m);
    }

    public void stopGame() {
        // Don't call in first turn (only every turn after)
        if (ongoing == true) {
            kill = true;
        }
    }

    public void prepGame() {
        // Clear TextView's
        secretNumber1.setText("----");
        secretNumber2.setText("----");
        player1.setText("Worker Thread 1");
        player2.setText("Worker Thread 2");

        // Clear excludes
        excludeDigits1.clear();
        excludeDigits2.clear();

        // Clear ListView's
        arrayList1.removeAll(arrayList1);
        listView1.setAdapter(arrayAdapter1);
        arrayList2.removeAll(arrayList2);
        listView2.setAdapter(arrayAdapter2);

        worker1Turn = true; // Worker Thread 1 always goes first
        turn = 1; // Turn 1 (halt after 20)
    }

    public Map<String, Integer> evaluateGuess(int [] actual, int [] guess) {
        // Evaluation HashMap
        Map<String, Integer> evaluation = new HashMap<>();
        evaluation.put("correctPos", 0);
        evaluation.put("wrongPos", 0);
        evaluation.put("badGuess", -1);

        // Evaluation Step
        for (int i = 0; i < guess.length; i++) {
            for (int j = 0; j < actual.length; j++) {
                if (guess[i] == actual[j]) { // Good guess
                    if (i == j) { // Correct position
                        evaluation.put("correctPos", evaluation.get("correctPos") + 1);
                    } else { // Wrong position
                        evaluation.put("wrongPos", evaluation.get("wrongPos") + 1);
                    }
                    break; // b/c digit is unique
                } else if (j == (actual.length - 1)) { // Bad guess
                    evaluation.put("badGuess", guess[i]);
                }
            }
        }

        return evaluation;
    }
}
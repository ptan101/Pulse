package tan.philip.nrf_ble.GraphScreen.UIComponents;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

import tan.philip.nrf_ble.GraphScreen.GraphSignal;

public class DigitalDisplayManager {
    private final ConstraintLayout layout;
    private final ArrayList<DigitalDisplay> digitalDisplays;
    private final ArrayList<TableRow> rows;
    private final Context ctx;

    public DigitalDisplayManager(Context ctx, ConstraintLayout layout) {
        this.ctx = ctx;
        this.layout = layout;
        digitalDisplays = new ArrayList<>();
        rows = new ArrayList<>();
    }

    public void addToDigitalDisplay(DigitalDisplay display) {
        display.setId(View.generateViewId());
        ConstraintSet set = new ConstraintSet();
        layout.addView(display);
        set.clone(layout);

        if(digitalDisplays.size() == 0) {
            set.connect(display.getId(), ConstraintSet.TOP, layout.getId(), ConstraintSet.TOP);
            set.connect(display.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START);
            set.connect(display.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END);
        } else {
            DigitalDisplay lastDisplay = digitalDisplays.get(digitalDisplays.size() - 1);

            if(digitalDisplays.size() % 2 == 0) {
                set.connect(display.getId(), ConstraintSet.TOP, lastDisplay.getId(), ConstraintSet.BOTTOM);
                set.connect(display.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START);
                set.connect(display.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END);
            } else {
                set.connect(lastDisplay.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START);
                set.connect(lastDisplay.getId(), ConstraintSet.END, display.getId(), ConstraintSet.START);

                set.connect(display.getId(), ConstraintSet.TOP, lastDisplay.getId(), ConstraintSet.TOP);
                set.connect(display.getId(), ConstraintSet.START, lastDisplay.getId(), ConstraintSet.END);
                set.connect(display.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END);
            }
        }
        digitalDisplays.add(display);
        set.applyTo(layout);
    }

    public void disableDigitalDisplay() {
        layout.setVisibility(View.GONE);
    }

    ////////////////////////////////////////////////////GETTERS/////////////////////////////////////
    public ArrayList<DigitalDisplay> getDigitalDisplays() {
        return digitalDisplays;
    }

    ////////////////////////////////////////////////////STATIC METHODS/////////////////////////////////////////////
    //Method to take parse a string into an equation.
    //Credit to Boann on Stack Overflow
    //https://stackoverflow.com/questions/3422673/how-to-evaluate-a-math-expression-given-in-string-form
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}

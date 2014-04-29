/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

/**
 *
 * @author Undis
 */

    import java.text.ParseException;

public class Calculator {

    int num1;
    int num2;
    char operand;

    public Calculator(String text) throws IllegalArgumentException {
        String[] splitText = text.trim().split(" ");
        if(splitText.length != 3){
            throw new IllegalArgumentException("Feil lengde på teksten. Må inneholde to tall med en operand i mellom, husk å skille med mellomrom");
        }
        try{
        num1 = Integer.parseInt(splitText[0]);
        num2 = Integer.parseInt(splitText[2]);
        } catch(NumberFormatException e){
            throw new NumberFormatException("Feil ved lesing av tall. Må ha følgende format: tall operand tall");
        }

        if (splitText[1].length() > 1) {
            throw new IllegalArgumentException("Feil ved operand");
        }
        operand = splitText[1].charAt(0);
        if (operand != '+' && operand != '-') {
            throw new IllegalArgumentException("Feil ved operand, kun + eller - er tillatt");
        }

    }

    public int calculate() {
        int response;
        if (operand == '+') {
            response = (num1) + (num2);
        } else {
            response = (num1) - (num2);
        }
        return response;

    }
}
       
        


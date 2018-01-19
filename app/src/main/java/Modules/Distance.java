package Modules;

/**
 * Created by Mai Thanh Hiep on 4/3/2016.
 */
public class Distance {
    public String text;
    public int value;

    public Distance(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public void addDistance(String txt, int val)
    {
        text+= txt;
        value+=val;
    }
}
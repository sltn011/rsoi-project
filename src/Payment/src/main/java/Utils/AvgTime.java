package Utils;

public class AvgTime {

    public static class Info
    {
        public float avgTime;
        public Info() {this.avgTime=0;}
    }


    private float avgTime;
    private long requests;
    private long elapsed;

    public AvgTime()
    {
        this.avgTime = 0;
        this.requests = 0;
        this.elapsed = 0;
    }

    public void begin()
    {
        elapsed = System.currentTimeMillis();
        requests++;
    }

    public void end()
    {
        elapsed = System.currentTimeMillis() - this.elapsed;
        avgTime = ((avgTime * (requests - 1)) + this.elapsed) / (float)requests;
    }

    public void add(float avg) {avgTime += avg; requests++;}

    public float get() {return avgTime;}
}

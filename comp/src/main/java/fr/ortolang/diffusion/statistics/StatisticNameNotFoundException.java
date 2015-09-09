package fr.ortolang.diffusion.statistics;

@SuppressWarnings("serial")
public class StatisticNameNotFoundException extends Exception {

    public StatisticNameNotFoundException() {
        super();
    }

    public StatisticNameNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatisticNameNotFoundException(String message) {
        super(message);
    }

    public StatisticNameNotFoundException(Throwable cause) {
        super(cause);
    }

}
package sim;

@SuppressWarnings("serial")
public class PositionCalcException extends Exception {

	public double errorCode = -999;
	
	public PositionCalcException(double exceptionCode) {
		errorCode = exceptionCode;
	}
}

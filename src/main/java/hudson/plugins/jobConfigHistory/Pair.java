package hudson.plugins.jobConfigHistory;

public class Pair<S,T> {
	public S first;
	public T second;

	public Pair(S first, T second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public String toString() {
		return "Pair<["
			+ first.toString()
			+ "], ["
			+ second.toString()
			+ "]>";
	}
}

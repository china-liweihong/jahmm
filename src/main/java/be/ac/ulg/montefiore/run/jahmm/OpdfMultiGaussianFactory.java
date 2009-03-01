package be.ac.ulg.montefiore.run.jahmm;


/**
 * This class can build <code>OpdfMultiGaussian</code> observation probability 
 * functions.
 */
public class OpdfMultiGaussianFactory 
implements OpdfFactory<OpdfMultiGaussian>
{	
	private int dimension;
	
	
	/**
	 * Generates a new multivariate gaussian observation probability 
	 * distribution function.
	 * 
	 * @param dimension The dimension of the vectors generated by this
	 *                  object.
	 */
	public OpdfMultiGaussianFactory(int dimension)
	{
		this.dimension = dimension;
	}
	
	
	public OpdfMultiGaussian factor()
	{
		return new OpdfMultiGaussian(dimension);
	}
}
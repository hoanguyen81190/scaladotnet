/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2008-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.util



import collection.Seq



/**
 * The `Try` type represents a computation that may either result in an exception, 
 * or return a success value. It's analagous to the `Either` type.
 */
sealed abstract class Try[+T] {
  /**
   * Returns true if the `Try` is a `Failure`, false otherwise.
   */
  def isFailure: Boolean

  /**
   * Returns true if the `Try` is a `Success`, false otherwise.
   */
  def isSuccess: Boolean

  /**
   * Returns the value from this `Success` or the given argument if this is a `Failure`.
   */
  def getOrElse[U >: T](default: => U) = if (isSuccess) get else default

  /**
   * Returns the value from this `Success` or throws the exception if this is a `Failure`.
   */
  def get: T

  /**
   * Applies the given function f if this is a Result.
   */
  def foreach[U](f: T => U): Unit

  /**
   * Returns the given function applied to the value from this `Success` or returns this if this is a `Failure`.
   */
  def flatMap[U](f: T => Try[U]): Try[U]

  /**
   * Maps the given function to the value from this `Success` or returns this if this is a `Failure`.
   */
  def map[U](f: T => U): Try[U]

  def collect[U](pf: PartialFunction[T, U]): Try[U]
  
  def exists(p: T => Boolean): Boolean
  
  /**
   * Converts this to a `Failure` if the predicate is not satisfied.
   */
  def filter(p: T => Boolean): Try[T]

  /**
   * Converts this to a `Failure` if the predicate is not satisfied.
   */
  def filterNot(p: T => Boolean): Try[T] = filter(x => !p(x))

  /**
   * Calls the exceptionHandler with the exception if this is a `Failure`. This is like `flatMap` for the exception.
   */
  def rescue[U >: T](rescueException: PartialFunction[System.Exception, Try[U]]): Try[U]

  /**
   * Calls the exceptionHandler with the exception if this is a `Failure`. This is like map for the exception.
   */
  def recover[U >: T](rescueException: PartialFunction[System.Exception, U]): Try[U]
  
  /**
   * Returns `None` if this is a `Failure` or a `Some` containing the value if this is a `Success`.
   */
  def toOption = if (isSuccess) Some(get) else None

  def toSeq = if (isSuccess) Seq(get) else Seq()
  
  /**
   * Returns the given function applied to the value from this Success or returns this if this is a `Failure`.
   * Alias for `flatMap`.
   */
  def andThen[U](f: T => Try[U]): Try[U] = flatMap(f)

  /**
   * Transforms a nested `Try`, i.e., a `Try` of type `Try[Try[T]]`, 
   * into an un-nested `Try`, i.e., a `Try` of type `Try[T]`.
   */
  def flatten[U](implicit ev: T <:< Try[U]): Try[U]
  
  def failed: Try[System.Exception]
}


final case class Failure[+T](val exception: System.Exception) extends Try[T] {
  def isFailure = true
  def isSuccess = false
  def rescue[U >: T](rescueException: PartialFunction[System.Exception, Try[U]]): Try[U] = {
    try {
      if (rescueException.isDefinedAt(exception)) rescueException(exception) else this
    } catch {
      case e2 => Failure(e2)
    }   
  }
  def get: T = throw exception
  def flatMap[U](f: T => Try[U]): Try[U] = Failure[U](exception)
  def flatten[U](implicit ev: T <:< Try[U]): Try[U] = Failure[U](exception)
  def foreach[U](f: T => U): Unit = {}
  def map[U](f: T => U): Try[U] = Failure[U](exception)
  def collect[U](pf: PartialFunction[T, U]): Try[U] = Failure[U](exception)
  def filter(p: T => Boolean): Try[T] = this
  def recover[U >: T](rescueException: PartialFunction[System.Exception, U]): Try[U] = 
    if (rescueException.isDefinedAt(exception)) {
      Try(rescueException(exception))
    } else {
      this
    }
  def exists(p: T => Boolean): Boolean = false
  def failed: Try[System.Exception] = Success(exception)
}


final case class Success[+T](r: T) extends Try[T] {
  def isFailure = false
  def isSuccess = true
  def rescue[U >: T](rescueException: PartialFunction[System.Exception, Try[U]]): Try[U] = Success(r)
  def get = r
  def flatMap[U](f: T => Try[U]): Try[U] = 
    try f(r) 
    catch { 
      case e => Failure(e) 
    }
  def flatten[U](implicit ev: T <:< Try[U]): Try[U] = r
  def foreach[U](f: T => U): Unit = f(r)
  def map[U](f: T => U): Try[U] = Try[U](f(r))
  def collect[U](pf: PartialFunction[T, U]): Try[U] =
    if (pf isDefinedAt r) Success(pf(r))
    else Failure[U](new NoSuchElementException("Partial function not defined at " + r))
  def filter(p: T => Boolean): Try[T] = 
    if (p(r)) this
    else Failure(new NoSuchElementException("Predicate does not hold for " + r))
  def recover[U >: T](rescueException: PartialFunction[System.Exception, U]): Try[U] = this
  def exists(p: T => Boolean): Boolean = p(r)
  def failed: Try[System.Exception] = Failure(new java.lang.UnsupportedOperationException("Success.failed"))
}


object Try {
  
  def apply[T](r: => T): Try[T] = {
    try { Success(r) } catch {
      case e => Failure(e)
    }
  }
  
}

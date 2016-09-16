package exceptions

case class NoEnrolmentException(message: String = "", cause: Throwable = null) extends Exception(message,cause)

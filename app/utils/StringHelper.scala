package utils

object StringHelper {

  def isAllDigits(x: String) = x forall Character.isDigit

}
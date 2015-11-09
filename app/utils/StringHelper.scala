package utils

import play.api.i18n.Messages
import scala.collection.mutable.ListBuffer

object StringHelper {

  def isAllDigits(x: String) = x forall Character.isDigit

}
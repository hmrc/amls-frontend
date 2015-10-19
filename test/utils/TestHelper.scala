package utils
import play.api.data.FormError

/**
 * Created by grant on 19/10/15.
 */
object TestHelper {
  /**
   * Check that Either Left result has specified key value.
   * @param result
   * @param messageKey
   * @return
   */
  def isErrorMessageKeyEqual(result: Either[Seq[FormError], String], messageKey:String) = {
    result match {
      case Left(List(FormError(a, b, c))) if b.contains(messageKey) => true
      case _ => false
    }
  }
}

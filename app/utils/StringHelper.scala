package utils

object StringHelper {
  val StartOfPrefix = 0
  val EndOfPrefix = 2
  val SuffixCharacter = 8
  val FirstNumberStart = 2
  val FirstNumberEnd = 4
  val SecondNumberStart = 4
  val SecondNumberEnd = 6
  val ThirdNumberStart = 6
  val ThirdNumberEnd = 8


  implicit class StringImprovements(val s: String) {
    def ninoFormat = {

      if(s.length >= 9) {
        val str = s.replace(" ", "")
        (str.substring(StartOfPrefix, EndOfPrefix)
          + " " + str.substring(FirstNumberStart, FirstNumberEnd)
          + " " + str.substring(SecondNumberStart, SecondNumberEnd)
          + " " + str.substring(ThirdNumberStart, ThirdNumberEnd)
          + " " + str.substring(SuffixCharacter)).toUpperCase
      }else{
        s
      }
    }
  }

  def isAllDigits(x: String) = x forall Character.isDigit

}
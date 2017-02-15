package models

trait CharacterSets {

  private val digits = Set("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")

  private val alphaUpper = Set("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")

  private val alphaLower = Set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")

  private val extendedAlphaUpper = Set("À", "Á", "Â", "Ã", "Ä", "Å", "Æ", "Ç", "È", "É", "Ê", "Ë", "Ì", "Í", "Î", "Ï", "Ð", "Ñ", "Ò", "Ó", "Ô", "Õ", "Ö", "Ø", "Ù", "Ú", "Û", "Ü", "Ý", "Þ")

  private val extendedAlphaLower = Set("ß", "à", "á", "â", "ã", "ä", "å", "æ", "ç", "è", "é", "ê", "ë", "ì", "í", "î", "ï", "ð", "ñ", "ò", "ó", "ô", "õ", "ö", "ø", "ù", "ú", "û", "ü", "ý", "þ", "ÿ")

  private val symbols1 = Set(" ", "!", "#", "$", "%", "&", "'", "‘", "’", "\"", "“", "”", "«", "»", "(", ")", "*", "+", ",", "-", "-", "–", "—", ".", "/")

  private val symbols2 = Set(":", ";", "<", "=", ">", "?", "@")

  val telephone = digits ++ Set(" ", "(", ")", "+", "-", "-")

  val reference = digits ++ alphaUpper ++ alphaLower

  val extendedReference = digits ++ alphaUpper ++ alphaLower ++ Set(" ", "-", "-")

  val companyNames = symbols1 ++ digits ++ symbols2 ++ alphaUpper ++ alphaLower ++ extendedAlphaUpper ++ extendedAlphaLower ++ Set(
    "[", "\\", "]", "{", "}", "£", "€", "¥", "÷", "×"
  )

  val tradingNames = symbols1 ++ digits ++ symbols2 ++ alphaUpper ++ alphaLower ++ extendedAlphaUpper ++ extendedAlphaLower ++ Set(
    "[", "\\", "]", "^", "_", "`", "{", "|", "}", "~", "£", "€", "¥", "÷", "×"
  )

  val individualNames = alphaUpper ++ alphaLower ++ extendedAlphaUpper ++ extendedAlphaLower ++ Set(
    " ", "'", "‘", "’", "-", "-", "–", "—"
  )

  val addresses = digits ++ alphaUpper ++ alphaLower ++ Set(
    " ", "!", "'", "‘", "’", "\"", "“", "”", "(", ")", ",", "-", "-", "–", "—", ".", "/"
  )

}

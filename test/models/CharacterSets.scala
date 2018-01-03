/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

trait CharacterSets {

  val digits = Set("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")

  val alphaUpper = Set("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")

  val alphaLower = Set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")

  val extendedAlphaUpper = Set("À", "Á", "Â", "Ã", "Ä", "Å", "Æ", "Ç", "È", "É", "Ê", "Ë", "Ì", "Í", "Î", "Ï", "Ð", "Ñ", "Ò", "Ó", "Ô", "Õ", "Ö", "Ø", "Ù", "Ú", "Û", "Ü", "Ý", "Þ")

  val extendedAlphaLower = Set("ß", "à", "á", "â", "ã", "ä", "å", "æ", "ç", "è", "é", "ê", "ë", "ì", "í", "î", "ï", "ð", "ñ", "ò", "ó", "ô", "õ", "ö", "ø", "ù", "ú", "û", "ü", "ý", "þ", "ÿ")

  val symbols1 = Set(" ", "!", "#", "$", "%", "&", "'", "‘", "’", "\"", "“", "”", "«", "»", "(", ")", "*", "+", ",", "-", "-", "–", "—", ".", "/")

  val symbols2 = Set(":", ";", "=", "?", "@")

  val symbols3 = Set(" ", "(", ")", "+", "-", "-")

  val symbols4 = Set(" ", "-", "-")

  val symbols5 = Set("[", "\\", "]", "{", "}", "£", "€", "¥", "÷", "×")

  val symbols6 = Set("[", "\\", "]", "^", "_", "`", "|", "~", "£", "€", "¥", "÷", "×")

  val symbols7 = Set(" ", "'", "‘", "’", "-", "-", "–", "—")

  val symbols8 = Set(" ", "!", "'", "‘", "’", "\"", "“", "”", "(", ")", ",", "-", "-", "–", "—", ".", "/").mkString("")

  val telephone = digits ++ symbols3

  val reference = digits ++ alphaUpper ++ alphaLower

  val extendedReference = digits ++ alphaUpper ++ alphaLower ++ symbols4

  val companyNames = symbols1 ++ digits ++ symbols2 ++ alphaUpper ++ alphaLower ++ extendedAlphaUpper ++ extendedAlphaLower ++ symbols5

  val tradingNames = symbols1 ++ digits ++ symbols2 ++ alphaUpper ++ alphaLower ++ extendedAlphaUpper ++ extendedAlphaLower ++ symbols6

  val individualNames = alphaUpper ++ alphaLower ++ extendedAlphaUpper ++ extendedAlphaLower ++ symbols7

  val addresses = (digits ++ alphaUpper ++ alphaLower ++ symbols8).mkString("")

}

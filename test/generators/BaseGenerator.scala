/*
 * Copyright 2024 HM Revenue & Customs
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

package generators

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaChar, alphaNumChar, listOfN, numChar}

import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDate}

//noinspection ScalaStyle
trait BaseGenerator {

  def stringOfLengthGen(maxLength: Int): Gen[String] =
    Gen.listOfN(maxLength, Gen.alphaNumChar).map(x => x.mkString)

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    length <- Gen.chooseNum(minLength + 1, (minLength * 2).max(100))
    chars  <- listOfN(length, alphaNumChar)
  } yield chars.mkString

  def stringsShorterThan(minLength: Int): Gen[String] = for {
    length <- Gen.chooseNum(0, minLength - 1)
    chars  <- listOfN(length, alphaNumChar)
  } yield chars.mkString

  def alphaStringsShorterThan(minLength: Int): Gen[String] = for {
    length <- Gen.chooseNum(0, minLength - 1)
    chars  <- listOfN(length, alphaChar)
  } yield chars.mkString

  def numStringOfLength(length: Int): Gen[String] = for {
    chars <- listOfN(length, numChar)
  } yield chars.mkString

  def numSequence(maxLength: Int): Gen[String] =
    Gen.listOfN(maxLength, Gen.chooseNum(1, 9)) map { _.mkString }

  def numsLongerThan(length: Int): Gen[Int]  =
    Gen.listOfN(length + 1, Gen.chooseNum(1, 9)).map(_.mkString.toInt)
  def numsShorterThan(length: Int): Gen[Int] =
    Gen.listOfN(length - 1, Gen.chooseNum(1, 9)).map(_.mkString.toInt)

  def numGen: Gen[Int] = Gen.chooseNum(0, 1000)

  val paymentAmountGen: Gen[Double] = Gen.chooseNum[Double](100, 200)

  val localDateGen: Gen[LocalDate] = for {
    day   <- Gen.chooseNum(1, 27)
    month <- Gen.chooseNum(1, 12)
    year  <- Gen.chooseNum(1990, 2016)
  } yield LocalDate.of(year, month, day)

  def safeIdGen: Gen[String] = for {
    ref <- stringOfLengthGen(9)
  } yield s"X${ref.toUpperCase}"

  val postcodeGen: Gen[String] = for {
    a    <- stringOfLengthGen(2)
    num1 <- Gen.chooseNum(1, 99)
    num2 <- Gen.chooseNum(1, 9)
    b    <- stringOfLengthGen(2)
  } yield s"$a$num1 $num2$b"

  val emailGen: Gen[String] = for {
    prefix <- stringOfLengthGen(6)
    suffix <- stringOfLengthGen(15)
  } yield s"$prefix@$suffix.com"

  val nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  val invalidChar: Gen[String] =
    Gen
      .oneOf[String](
        Seq(
          "@",
          "£",
          "$",
          "%",
          "^",
          "&",
          "*",
          "~",
          ">",
          "<",
          "|",
          "]",
          "[",
          "}",
          "{",
          "=",
          "+"
        )
      )
      .suchThat(_.nonEmpty)

  val invalidCharForNames: Gen[String] =
    Gen
      .oneOf[String](
        Seq(
          "ƒ",
          "„",
          "…",
          "†",
          "‡",
          "ˆ",
          "‰",
          "‹",
          "Œ",
          "•",
          "™",
          "œ",
          "¡",
          "¢",
          "¤",
          "¦",
          "§",
          "¨",
          "©",
          "ª",
          "¬",
          "®",
          "°",
          "±",
          "²",
          "³",
          "¶",
          "¸",
          "¹",
          "º",
          "¼",
          "½",
          "¾",
          "¿"
        )
      )
      .suchThat(_.nonEmpty)

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long = date.atStartOfDay().toInstant(UTC).toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map { millis =>
      LocalDate.ofInstant(Instant.ofEpochMilli(millis), UTC)
    }
  }
}

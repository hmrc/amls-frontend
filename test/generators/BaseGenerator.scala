/*
 * Copyright 2023 HM Revenue & Customs
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

import org.joda.time.{LocalDate => JodaLocalDate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaNumChar, alphaStr, listOfN, numChar}

import java.time.{LocalDate, ZoneId}

//noinspection ScalaStyle
trait BaseGenerator {

  def stringOfLengthGen(maxLength: Int): Gen[String] = {
    Gen.listOfN(maxLength, Gen.alphaNumChar).map(x => x.mkString)
  }

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    length    <- Gen.chooseNum(minLength + 1, (minLength * 2).max(100))
    chars     <- listOfN(length, alphaNumChar)
  } yield chars.mkString

  def stringsShorterThan(minLength: Int): Gen[String] = for {
    length    <- Gen.chooseNum(0, minLength - 1)
    chars     <- listOfN(length, alphaNumChar)
  } yield chars.mkString

  def numStringOfLength(length: Int): Gen[String] = for {
    chars <- listOfN(length, numChar)
  } yield chars.mkString

  def numSequence(maxLength: Int) =
    Gen.listOfN(maxLength, Gen.chooseNum(1, 9)) map {_.mkString}

  def numsLongerThan(length: Int): Gen[Int] =
    Gen.listOfN(length + 1, Gen.chooseNum(1, 9)).map(_.mkString.toInt)
  def numsShorterThan(length: Int): Gen[Int] =
    Gen.listOfN(length - 1, Gen.chooseNum(1, 9)).map(_.mkString.toInt)


  def numGen = Gen.chooseNum(0,1000)

  val paymentAmountGen = Gen.chooseNum[Double](100, 200)

  val localDateGen: Gen[LocalDate] = for {
    day <- Gen.chooseNum(1, 27)
    month <- Gen.chooseNum(1, 12)
    year <- Gen.chooseNum(1990, 2016)
  } yield LocalDate.of(year, month, day)

  val jodaLocalDateGen: Gen[JodaLocalDate] = for {
    day <- Gen.chooseNum(1, 27)
    month <- Gen.chooseNum(1, 12)
    year <- Gen.chooseNum(1990, 2016)
  } yield new JodaLocalDate(year, month, day)

  def safeIdGen = for {
    ref <- stringOfLengthGen(9)
  } yield s"X${ref.toUpperCase}"

  val postcodeGen: Gen[String] = for {
    a <- stringOfLengthGen(2)
    num1 <- Gen.chooseNum(1, 99)
    num2 <- Gen.chooseNum(1, 9)
    b <- stringOfLengthGen(2)
  } yield s"$a$num1 $num2$b"

  val emailGen: Gen[String] = for {
    prefix <- stringOfLengthGen(6)
    suffix <- stringOfLengthGen(15)
  } yield s"$prefix@$suffix.com"

  val nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat (_.nonEmpty)
      .suchThat (_ != "true")
      .suchThat (_ != "false")

  def jodaDatesBetween(min: JodaLocalDate, max: JodaLocalDate): Gen[JodaLocalDate] = {

    def toMillis(date: JodaLocalDate): Long =
      date.toDateTimeAtStartOfDay.toInstant.getMillis

    Gen.choose(toMillis(min), toMillis(max)).map {
      millis => new JodaLocalDate(millis)
    }
  }
}

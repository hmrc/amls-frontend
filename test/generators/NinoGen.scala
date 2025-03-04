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

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.domain.Nino

trait NinoGen {
  val invalidPrefixes                  = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
  val validFirstCharacters: List[Char] = ('A' to 'Z').filterNot(List('D', 'F', 'I', 'Q', 'U', 'V').contains).toList

  val validSecondCharacters: List[Char] =
    ('A' to 'Z').filterNot(List('D', 'F', 'I', 'O', 'Q', 'U', 'V').contains).toList

  val validPrefixes: List[String] = {
    for {
      c1 <- validFirstCharacters
      c2 <- validSecondCharacters
    } yield s"$c1$c2"
  }.filterNot(invalidPrefixes.contains)

  val genNumPart: Gen[String] = Gen.listOfN(6, Gen.oneOf('0' to '9')).map(_.mkString)

  val ninoGen: Gen[Nino] = {
    for {
      prefix  <- Gen.oneOf(validPrefixes)
      numPart <- genNumPart
      suffix  <- Gen.oneOf('A' to 'D')
    } yield s"$prefix$numPart$suffix"
  }.map(Nino(_))

  implicit val arbNino: Arbitrary[Nino] = Arbitrary(ninoGen)
}

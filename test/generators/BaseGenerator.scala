/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalacheck.Gen

trait BaseGenerator {

  def stringOfLengthGen(maxLength: Int) = {
    Gen.listOfN(maxLength, Gen.alphaNumChar).map(x => x.mkString)
  }

  def alphaNumOfLengthGen(maxLength: Int) = {
    Gen.listOfN(maxLength, Gen.alphaNumChar).map(x => x.mkString)
  }

  def numGen = Gen.chooseNum(0,1000)

  def safeIdGen = for {
    ref <- alphaNumOfLengthGen(9)
  } yield s"X${ref.toUpperCase}"

}

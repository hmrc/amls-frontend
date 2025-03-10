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

package matchers.table

import org.scalatest.matchers.{MatchResult, Matcher}
import uk.gov.hmrc.govukfrontend.views
import uk.gov.hmrc.govukfrontend.views.Aliases
import uk.gov.hmrc.govukfrontend.views.Aliases.Table
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.HeadCell

trait TableMatchers {

  /** Table has given number of rows
    */
  class TableNumRowsMatcher(numRows: Int) extends Matcher[Table] {
    override def apply(left: Table): MatchResult =
      MatchResult(left.rows.size == numRows, s"Table has ${left.rows} not $numRows", s"Table has $numRows")
  }

  def haveNumRows(expectedNumRows: Int) = new TableNumRowsMatcher(expectedNumRows)

  /** Table has given table headers
    */
  class TableHeaderMatcher(tableHeaders: Seq[HeadCell]) extends Matcher[Table] {
    override def apply(left: Table): MatchResult = {
      if (left.head.isEmpty && tableHeaders.nonEmpty) {
        MatchResult(false, s"Table contained no headers but provided headers are: ${tableHeaders.mkString}", "N/A")
      }

      if (left.head.nonEmpty && tableHeaders.isEmpty) {
        MatchResult(
          false,
          s"Table contained the following headers: ${left.head.get.mkString} but empty header sequence provided",
          "N/A"
        )
      }

      val nonMatchingHeadCells = tableHeaders.foldLeft(Seq.empty[HeadCell]) {
        (nonMatchingHeadCells: Seq[HeadCell], curr: HeadCell) =>
          if (!left.head.get.contains(curr)) {
            nonMatchingHeadCells.:+(curr)
          } else {
            nonMatchingHeadCells
          }
      }

      MatchResult(
        nonMatchingHeadCells.isEmpty,
        s"Table did not contain the following headers: ${nonMatchingHeadCells.mkString}",
        "Table has all of the headers"
      )
    }
  }

  def haveTableHeaders(tableHeaders: Seq[HeadCell]) = new TableHeaderMatcher(tableHeaders)

  /** Table has no headers
    */
  class TableNoHeaderMatcher() extends Matcher[Table] {
    override def apply(left: Aliases.Table): MatchResult =
      MatchResult(
        !left.head.exists(_.nonEmpty),
        "Table has no headers",
        s"Table has the following headers: ${left.head}"
      )
  }

  def haveNoTableHeaders = new TableNoHeaderMatcher()

  /** Table has no rows
    */
  class TableNoRowsMatcher() extends Matcher[Table] {
    override def apply(left: views.Aliases.Table): MatchResult =
      MatchResult(left.rows.isEmpty, s"Table contained ${left.rows.size} rows", "Table has no rows")
  }

  def haveNoRows = new TableNoRowsMatcher()
}

object TableMatchers extends TableMatchers

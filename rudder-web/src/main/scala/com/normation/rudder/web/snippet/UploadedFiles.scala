/*
*************************************************************************************
* Copyright 2011 Normation SAS
*************************************************************************************
*
* This file is part of Rudder.
* 
* Rudder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* In accordance with the terms of section 7 (7. Additional Terms.) of
* the GNU General Public License version 3, the copyright holders add
* the following Additional permissions:
* Notwithstanding to the terms of section 5 (5. Conveying Modified Source
* Versions) and 6 (6. Conveying Non-Source Forms.) of the GNU General
* Public License version 3, when you create a Related Module, this
* Related Module is not considered as a part of the work and may be
* distributed under the license agreement of your choice.
* A "Related Module" means a set of sources files including their
* documentation that, without modification of the Source Code, enables
* supplementary functions or services in addition to those offered by
* the Software.
* 
* Rudder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Rudder.  If not, see <http://www.gnu.org/licenses/>.

*
*************************************************************************************
*/

package com.normation.rudder.web.snippet

import org.slf4j.LoggerFactory
import java.io.{FileOutputStream,File}
import com.normation.inventory.domain.MemorySize
import org.joda.time.DateTime
import com.normation.rudder.services.system.FileManager
import scala.xml._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util._
import Helpers._
import net.liftweb.http.js._
import JsCmds._
import JE._
import net.liftweb.http.SHtml._
import bootstrap.liftweb.RudderConfig

/*
 * A simple snippet that display the content of
 * a (local to server) directory and allows
 * to add (by upload)/remove file in it
 */

class UploadedFiles {
  private[this] val fileManager = RudderConfig.fileManager

  def list(html:NodeSeq) : NodeSeq = {
    def deleteFiles() : Unit = {
      S.params("filesToDelete").foreach { name =>
        fileManager.getFiles.filter( _.getName == name).foreach(_.delete)
      }
      S.redirectTo(S.uri)
    }

    bind("files",html,
      "delete" -> SHtml.submit("Delete", deleteFiles),
      "lines" -> (fileManager.getFiles.flatMap { f =>
            //build all table lines
            bind("line",chooseTemplate("files","lines",html),
              "name" -> f.getName,
              "size" -> new MemorySize(f.length).toStringMo,
              "date" -> new DateTime(f.lastModified).toString("YYYY-MM-dd HH:mm:ss"),
              "delete" -> <input type="checkbox" name="filesToDelete" value={f.getName} />
            )
          } ++ Script(OnLoad(JsRaw(""" $('#uploaded_files_grid').dataTable({
              "bJQueryUI": false, "bPaginate": false, "bInfo":false,
              "bAutoWidth": false, "asStripeClasses": [ 'color1', 'color2' ],
              "aoColumns": [
                { "sWidth": "500px" },
                { "sWidth": "150px" },
                { "sWidth": "150px" },
                { "sWidth": "20px", "bSortable": false },
              ]
            });moveFilterAndPaginateArea('#uploaded_files_grid');"""))
        )
      )
    )
  }

  def upload(html:NodeSeq) : NodeSeq = {
    var fileHolder : Box[FileParamHolder] = Empty

    def uploadFiles() : Unit = {
      fileHolder match {
        case Full(fh) => fileManager.copyFileToRootDir(fh)
        case _ => // nothing to do
      }
      S.redirectTo(S.uri)
    }

    bind("files",html,
      "select" -> SHtml.fileUpload(x => fileHolder = Full(x)),
      "upload" -> SHtml.submit("Upload", uploadFiles)
    )
  }
}

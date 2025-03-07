// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.spark.rdd

import org.apache.doris.spark.client.entity.DorisReaderPartition
import org.apache.doris.spark.client.read.DorisReader
import org.apache.doris.spark.config.{DorisConfig, DorisOptions}
import org.apache.spark.util.TaskCompletionListener
import org.apache.spark.{TaskContext, TaskKilledException}
import org.slf4j.{Logger, LoggerFactory}

private[spark] abstract class AbstractDorisRDDIterator[T](
    context: TaskContext,
    partition: DorisReaderPartition) extends Iterator[T] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
  private var initialized = false
  private var closed = false

  // the reader obtain data from Doris BE
  private lazy val reader = {
    initialized = true
    val config = partition.getConfig
    initReader(config)
    val valueReaderName = config.getValue(DorisOptions.DORIS_VALUE_READER_CLASS)
    logger.debug(s"Use value reader '$valueReaderName'.")
    val cons = Class.forName(valueReaderName).getDeclaredConstructor(classOf[DorisReaderPartition])
    cons.newInstance(partition).asInstanceOf[DorisReader]
  }

  context.addTaskCompletionListener(new TaskCompletionListener() {
    override def onTaskCompletion(context: TaskContext): Unit = {
      closeIfNeeded()
    }
  })

  override def hasNext: Boolean = {
    if (context.isInterrupted()) {
      throw new TaskKilledException
    }
    reader.hasNext
  }

  override def next(): T = {
    if (!hasNext) {
      throw new NoSuchElementException("End of stream")
    }
    val value = reader.next()
    createValue(value)
  }

  private def closeIfNeeded(): Unit = {
    logger.trace(s"Close status is '$closed' when close Doris RDD Iterator")
    if (!closed) {
      close()
      closed = true
    }
  }

  protected def close(): Unit = {
    logger.trace(s"Initialize status is '$initialized' when close Doris RDD Iterator")
    if (initialized) {
      reader.close()
    }
  }

  def initReader(config: DorisConfig): Unit

  /**
   * convert value of row from reader.next return type to T.
   * @param value reader.next return value
   * @return value of type T
   */
  def createValue(value: Object): T
}

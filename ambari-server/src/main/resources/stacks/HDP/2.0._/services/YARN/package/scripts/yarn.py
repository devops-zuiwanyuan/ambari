#!/usr/bin/env python2.6
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""

from resource_management import *
import sys


def yarn():
  import params

  Directory([params.yarn_pid_dir, params.yarn_log_dir],
            owner=params.yarn_user,
            group=params.user_group,
            recursive=True
  )

  Directory([params.mapred_pid_dir, params.mapred_log_dir],
            owner=params.mapred_user,
            group=params.user_group,
            recursive=True
  )

  Directory([params.nm_local_dirs, params.nm_log_dirs, params.yarn_log_dir_prefix],
            owner=params.yarn_user,
            recursive=True
  )

  XmlConfig("core-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['core-site'],
            owner=params.hdfs_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("mapred-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['mapred-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("yarn-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['yarn-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("capacity-scheduler.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['capacity-scheduler'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  File(params.yarn_job_summary_log,
       owner=params.yarn_user,
       group=params.user_group
  )

  File(params.mapred_job_summary_log,
       owner=params.mapred_user,
       group=params.user_group
  )

  File(format("{limits_conf_dir}/yarn.conf"),
       mode=0644,
       content=Template('yarn.conf.j2')
  )

  File(format("{limits_conf_dir}/mapreduce.conf"),
       mode=0644,
       content=Template('mapreduce.conf.j2')
  )

  File(format("{config_dir}/yarn-env.sh"),
       owner=params.yarn_user,
       group=params.user_group,
       mode=0755,
       content=Template('yarn-env.sh.j2')
  )

  File(format("{config_dir}/hadoop-env.sh"),
       owner=params.hdfs_user,
       group=params.user_group,
       mode=0755,
       content=StaticFile(format('{hadoop_conf_dir}/hadoop-env.sh'))
  )

  if params.security_enabled:
    container_executor = format("{yarn_container_bin}/container-executor")
    File(container_executor,
         group=params.yarn_executor_container_group,
         mode=6050
    )

    Execute(format("chmod 6050 {yarn_container_bin}/container-executor"))   #Fix File permission setting

    File(format("{config_dir}/container-executor.cfg"),
         group=params.user_group,
         mode=0644,
         content=Template('container-executor.cfg.j2')
    )


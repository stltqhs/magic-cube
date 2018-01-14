# magic-log-config
日志配置

### logback
需要提供<code>logback.properties</code>文件。内容如下：
```
# 日志配置目录
LOG_HOME=/tmp/logs
# 日志文件名
FILE_NAME=app
# 日志最小级别
LOG_LEVEL=DEBUG
# 是否需要将日志输出到console，只是Debug级别的日志才输出到console
ENABLE_CONSOLE=true
# 文件日志保留时间，单位天
MAX_HISTORY=30
```
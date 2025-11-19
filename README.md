一个 完整的 Spring Boot 项目方案，满足以下需求：
✅ 对接开源节假日 API（holiday-cn）

✅ 自动缓存数据（避免重复请求）

✅ 提供 REST API 接口：GET /api/holiday/{year}

✅ 支持将结果保存为本地 JSON 文件（可选）

✅ 线程安全 + 异常处理 + 日志记录

项目目录：
src/
├── main/
│   ├── java/
│   │   └── com/lfw/holiday/
│   │       ├── HolidayApplication.java
│   │       ├── config/
│   │       │   └── WebMvcConfig.java
│   │       ├── controller/
│   │       │   └── HolidayController.java
│   │       ├── model/
│   │       │   ├── HolidayApiResponse.java
│   │       │   ├── HolidayDay.java
│   │       │   ├── OutputHolidayInfo.java
│   │       │   └── HolidayResponse.java
│   │       ├── service/
│   │       │   └── HolidayService.java
│   │       └── scheduler/
│   │           └── HolidayCacheScheduler.java
│   └── resources/
│       └── application.yml


**使用说明**

访问 Swagger UI：http://localhost:8086/swagger-ui.html

⭐️ 访问接口：GET http://localhost:8086/holiday?year=2024   [年份可以自己传2020 ~ 2035的，后续会自动更新]

前端跨域调用（无 CORS 错误）：

编辑
fetch('http://localhost:8086/holiday?year=2025')
.then(res => res.json())
.then(data => console.log(data));

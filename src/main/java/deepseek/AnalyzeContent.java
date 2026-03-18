package deepseek;

public class AnalyzeContent {
    public static String analyze(int type,String sqlQuery) {
        switch ( type){
            case 1:
                return "请分析以下SQL查询，并提供优化建议：\n" + sqlQuery +
                        "\n请以标准json结构返回结果，包含：" +
                        "1.optimized_sql : 优化后的SQL," +
                        "2.analysis_points : 分析要点," +
                        "3.optimization_suggestions : 优化建议";
            case 2:
                return sqlQuery ;
            case 3:
                return "SQL语句分析";
            case 4:
                return "SQL语句优化建议";
            case 5:
                return "SQL语句分析结果";
            case 6:
                return "SQL语句优化建议结果";
            case 7:
                return "SQL语句分析结果";
            case 8:
                return "SQL语句优化建议结果";
        }

        return "未知";



    }
}
package deepseek;

public class DebugPractice {
    // 主方法：程序入口
    public static void main(String[] args) {
        // 测试数组：包含正数、负数、零
        int[] numbers = {2, 5, 8, -4, 0, 9, 12};
        // 调用计算偶数平均值的方法
        double average = calculateEvenAverage(numbers);
        // 输出结果
        System.out.println("数组中偶数的平均值是：" + average);
    }

    /**
     * 计算数组中所有偶数的平均值
     * @param arr 输入的整数数组
     * @return 偶数的平均值
     */
    public static double calculateEvenAverage(int[] arr) {
        // 初始化总和和计数
        int sum = 0;
        int count = 0;

        // 遍历数组
        for (int i = 0; i < arr.length; i++) { // Bug 1：循环边界错误
            int num = arr[i];
            // 判断是否为偶数
            if (num % 2 == 1) { // Bug 2：偶数判断逻辑错误
                sum += num;
                count++;
                //System.out.println(num);
            }
        }
        //System.out.println(sum);

        // 计算平均值（避免除零错误）
        if (count == 0) {
            return 0.0;
        }
        return sum / count; // 隐含小问题：整数除法，可在Debug中发现
    }
}

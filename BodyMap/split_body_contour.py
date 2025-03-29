import json
import os
import numpy as np
import matplotlib.pyplot as plt

def load_contour_points(json_path):
    """
    加载轮廓点坐标
    
    参数:
        json_path: JSON文件路径
    
    返回:
        contour_points: 轮廓坐标点列表
    """
    with open(json_path, 'r') as f:
        contour_points = json.load(f)
    
    print(f"已加载 {len(contour_points)} 个轮廓点")
    return contour_points

def split_body_parts(contour_points):
    """
    按照指定的索引范围将轮廓点分割成不同的身体部位
    
    参数:
        contour_points: 轮廓坐标点列表
    
    返回:
        body_parts: 按部位划分的轮廓点字典
    """
    # 定义各部位的索引范围
    body_parts = {
        "头部": [*range(0, 9), *range(111, 120)],
        "颈部": [*range(8, 11), *range(109, 112)],
        "左肩膀": [*range(11, 17), 38],
        "左臂": [*range(16, 21), *range(33, 39)],
        "左手": [*range(20, 34)],
        "上身": [10, 11, *range(38, 45), 61, 62, 63, *range(79, 84), 108, 109],
        "左腿": [*range(44, 50), *range(54, 62)],
        "左脚": [*range(49, 55)],
        "右腿": [*range(63, 71), *range(74, 80)],
        "右脚": [*range(70, 75)],
        "右肩膀": [*range(103, 109), 83],
        "右臂": [*range(83, 89), *range(99, 104)],
        "右手": [*range(88, 100)]
    }
    
    # 创建各部位的点集
    parts_points = {}
    for part_name, indices in body_parts.items():
        parts_points[part_name] = [contour_points[i] for i in indices]
        print(f"{part_name}: {len(parts_points[part_name])} 个点")
    
    return parts_points

def save_body_parts(parts_points, output_dir):
    """
    保存各部位的轮廓点到单独的文件
    
    参数:
        parts_points: 按部位划分的轮廓点字典
        output_dir: 输出目录
    """
    os.makedirs(output_dir, exist_ok=True)
    
    for part_name, points in parts_points.items():
        output_path = os.path.join(output_dir, f"{part_name}.json")
        with open(output_path, 'w') as f:
            json.dump(points, f, ensure_ascii=False, indent=2)
        print(f"已保存 {part_name} 的轮廓点到: {output_path}")

def visualize_body_parts(contour_points, parts_points, output_dir):
    """
    可视化各部位的轮廓点
    
    参数:
        contour_points: 原始轮廓点列表
        parts_points: 按部位划分的轮廓点字典
        output_dir: 输出目录
    """
    os.makedirs(output_dir, exist_ok=True)
    
    # 定义不同部位的颜色
    colors = {
        "头部": (255, 0, 0),    # 红色
        "颈部": (255, 165, 0),  # 橙色
        "左肩膀": (255, 255, 0), # 黄色
        "左臂": (0, 255, 0),    # 绿色
        "左手": (0, 255, 255),  # 青色
        "上身": (0, 0, 255),    # 蓝色
        "左腿": (128, 0, 128),  # 紫色
        "左脚": (255, 0, 255),  # 粉色
        "右腿": (139, 69, 19),  # 棕色
        "右脚": (0, 128, 128),  # 青绿色
        "右肩膀": (128, 128, 0), # 橄榄色
        "右臂": (128, 0, 0),    # 深红色
        "右手": (0, 128, 0)     # 深绿色
    }
    
    # 创建一个白色背景的图像
    plt.figure(figsize=(15, 15))
    
    # 绘制原始轮廓点
    contour_array = np.array(contour_points)
    plt.plot(contour_array[:, 0], contour_array[:, 1], 'k-', linewidth=1, alpha=0.3)
    
    # 为每个点标注序号
    for i, point in enumerate(contour_points):
        plt.text(point[0], point[1], str(i), fontsize=8, color='black',
                 bbox=dict(facecolor='white', alpha=0.7, edgecolor='none', pad=1))
    
    # 绘制各部位的轮廓点
    for part_name, points in parts_points.items():
        points_array = np.array(points)
        color_rgb = [x/255 for x in colors[part_name]]
        plt.scatter(points_array[:, 0], points_array[:, 1], color=color_rgb, s=50, label=part_name)
    
    plt.legend(loc='upper right', bbox_to_anchor=(1.3, 1))
    plt.title("人体各部位轮廓点", fontsize=16)
    plt.axis('off')
    plt.tight_layout()
    
    # 修正图像方向
    plt.gca().invert_yaxis()  # 反转Y轴，使图像方向正确
    
    # 保存可视化结果
    output_path = os.path.join(output_dir, "body_parts_visualization.png")
    plt.savefig(output_path, dpi=300, bbox_inches='tight', pad_inches=0.1)
    print(f"已保存可视化结果到: {output_path}")
    
    # 创建单独的部位可视化图像
    for part_name, points in parts_points.items():
        plt.figure(figsize=(10, 10))
        
        # 绘制原始轮廓点（淡色）
        plt.plot(contour_array[:, 0], contour_array[:, 1], 'k-', linewidth=1, alpha=0.1)
        
        # 绘制当前部位的点
        points_array = np.array(points)
        color_rgb = [x/255 for x in colors[part_name]]
        plt.scatter(points_array[:, 0], points_array[:, 1], color=color_rgb, s=100)
        
        # 连接当前部位的点
        if len(points) > 1:
            plt.plot(points_array[:, 0], points_array[:, 1], '-', color=color_rgb, linewidth=2)
        
        # 为每个点标注序号
        for i, point in enumerate(points):
            plt.text(point[0], point[1], str(i), fontsize=10, color='black',
                     bbox=dict(facecolor='white', alpha=0.7, edgecolor='none', pad=1))
        
        plt.title(f"{part_name}轮廓点", fontsize=16)
        plt.axis('off')
        plt.tight_layout()
        
        # 修正图像方向
        plt.gca().invert_yaxis()  # 反转Y轴，使图像方向正确
        
        # 保存部位可视化结果
        part_output_path = os.path.join(output_dir, f"{part_name}_visualization.png")
        plt.savefig(part_output_path, dpi=300, bbox_inches='tight', pad_inches=0.1)
        print(f"已保存 {part_name} 可视化结果到: {part_output_path}")
        plt.close()

if __name__ == "__main__":
    # 设置输入文件路径和输出目录
    contour_json_path = "/Users/macpro/Downloads/BodyMap/output/body_red_2_contour.json"
    output_dir = "/Users/macpro/Downloads/BodyMap/output/body_parts"
    
    # 加载轮廓点
    contour_points = load_contour_points(contour_json_path)
    
    # 分割身体部位
    parts_points = split_body_parts(contour_points)
    
    # 保存各部位的轮廓点
    save_body_parts(parts_points, output_dir)
    
    # 可视化各部位的轮廓点
    visualize_body_parts(contour_points, parts_points, output_dir)
    
    print("处理完成!")
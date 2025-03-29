import cv2
import numpy as np
import matplotlib.pyplot as plt
import os
import json

def extract_body_contour(img_path, output_dir=None):
    """
    从人体轮廓图中提取坐标
    
    参数:
        img_path: 轮廓图片路径
        output_dir: 输出目录，如果为None则不保存结果
    
    返回:
        contour_points: 轮廓坐标点列表
    """
    # 1. 图像读取 - 使用IMREAD_UNCHANGED保留透明通道
    image = cv2.imread(img_path, cv2.IMREAD_UNCHANGED)
    if image is None:
        raise FileNotFoundError(f"无法读取图片: {img_path}")
    
    # 检查图像是否有透明通道
    if image.shape[2] == 4:  # 如果有RGBA四个通道
        # 创建一个掩码，只保留非透明区域
        alpha_channel = image[:, :, 3]
        _, mask = cv2.threshold(alpha_channel, 1, 255, cv2.THRESH_BINARY)
        
        # 转换为RGB图像用于显示
        rgb_image = cv2.cvtColor(image[:, :, :3], cv2.COLOR_BGR2RGB)
        
        # 使用透明通道作为掩码
        binary = mask.copy()
    else:
        # 如果没有透明通道，按原来的方式处理
        rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        _, binary = cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)
    
    # 4. 查找轮廓
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    # 5. 找到最大轮廓（通常是人体）
    if len(contours) > 0:
        largest_contour = max(contours, key=cv2.contourArea)
        # 平滑轮廓
        epsilon = 0.0005 * cv2.arcLength(largest_contour, True)
        largest_contour = cv2.approxPolyDP(largest_contour, epsilon, True)
        contour_points = largest_contour.squeeze()
    else:
        largest_contour = np.array([])
        contour_points = np.array([])
    
    # 6. 可视化和保存结果
    if output_dir is not None:
        os.makedirs(output_dir, exist_ok=True)
        
        # 保存轮廓图像
        img_contour = np.zeros((image.shape[0], image.shape[1], 3), dtype=np.uint8)
        if len(largest_contour) > 0:
            cv2.drawContours(img_contour, [largest_contour], -1, (0,255,0), 2)
        
        # 创建可视化图像
        fig, axes = plt.subplots(1, 3, figsize=(15, 5))
        axes[0].imshow(rgb_image)  # 使用RGB图像而不是BGR
        axes[0].set_title('原始图像')
        axes[0].axis('off')
        
        axes[1].imshow(binary, cmap='gray')
        axes[1].set_title('二值化图像/透明通道')
        axes[1].axis('off')
        
        axes[2].imshow(img_contour)
        axes[2].set_title('提取的轮廓')
        axes[2].axis('off')
        
        plt.tight_layout()
        
        # 保存可视化结果
        base_name = os.path.splitext(os.path.basename(img_path))[0]
        plt.savefig(os.path.join(output_dir, f"{base_name}_visualization.png"))
        
        # 保存轮廓坐标为JSON
        contour_list = contour_points.tolist() if len(contour_points) > 0 else []
        with open(os.path.join(output_dir, f"{base_name}_contour.json"), 'w') as f:
            json.dump(contour_list, f)
        
        # 添加日志，输出轮廓点数量
        print(f"提取到的轮廓点数量: {len(contour_list)}")
        
        # 按照人体部位划分轮廓点
        if len(contour_points) > 0:
            print("开始按人体部位划分轮廓点...")
            body_parts = segment_body_parts(contour_points)
            
            # 输出每个部位的点数
            for part, points in body_parts.items():
                print(f"  {part}: {len(points)} 个点")
            
            # 保存按部位划分的轮廓点
            body_parts_path = os.path.join(output_dir, f"{base_name}_body_parts.json")
            with open(body_parts_path, 'w') as f:
                json.dump(body_parts, f)
            
            print(f"已保存按部位划分的轮廓坐标到: {body_parts_path}")
            
            # 可视化不同部位的轮廓点
            body_parts_img_path = os.path.join(output_dir, f"{base_name}_body_parts.png")
            print(f"开始生成按部位可视化的图像...")
            visualize_body_parts(rgb_image, body_parts, body_parts_img_path)
            print(f"已保存按部位可视化的图像到: {body_parts_img_path}")
            
        print(f"已保存轮廓坐标到: {os.path.join(output_dir, f'{base_name}_contour.json')}")
        print(f"已保存可视化结果到: {os.path.join(output_dir, f'{base_name}_visualization.png')}")
    
    return contour_points

def segment_body_parts(contour_points):
    """
    将轮廓点按照人体部位进行分类，允许相邻部位的顶点重叠
    
    参数:
        contour_points: 轮廓坐标点数组
    
    返回:
        body_parts: 按部位划分的轮廓点字典
    """
    # 获取轮廓点的总数
    n_points = len(contour_points)
    
    # 如果轮廓点不足，则无法划分
    if n_points < 12:
        return {"全身": contour_points.tolist()}
    
    # 计算各部位的点数范围（起始索引）
    # 添加重叠区域，确保相邻部位的连接处有共同的点
    overlap = 2  # 重叠点数
    
    head_start = 0
    head_end = int(n_points * 0.1)
    
    neck_start = head_end - overlap
    neck_end = int(n_points * 0.15)
    
    upper_body_start = neck_end - overlap
    upper_body_end = int(n_points * 0.35)
    
    right_arm_start = upper_body_end - overlap
    right_arm_end = int(n_points * 0.45)
    
    right_hand_start = right_arm_end - overlap
    right_hand_end = int(n_points * 0.5)
    
    right_leg_start = right_hand_end - overlap
    right_leg_end = int(n_points * 0.6)
    
    right_foot_start = right_leg_end - overlap
    right_foot_end = int(n_points * 0.65)
    
    left_foot_start = right_foot_end - overlap
    left_foot_end = int(n_points * 0.7)
    
    left_leg_start = left_foot_end - overlap
    left_leg_end = int(n_points * 0.8)
    
    left_hand_start = left_leg_end - overlap
    left_hand_end = int(n_points * 0.85)
    
    left_arm_start = left_hand_end - overlap
    left_arm_end = n_points
    
    # 确保头部和左臂的连接处也有重叠
    if left_arm_end > n_points - 1:
        left_arm_points = list(range(left_arm_start, n_points))
        # 添加头部开始的几个点，形成闭环
        left_arm_points.extend(list(range(0, overlap)))
    else:
        left_arm_points = list(range(left_arm_start, left_arm_end))
    
    # 创建各部位的点集
    body_parts = {
        "头部": contour_points[head_start:head_end].tolist(),
        "颈部": contour_points[neck_start:neck_end].tolist(),
        "上身": contour_points[upper_body_start:upper_body_end].tolist(),
        "右臂": contour_points[right_arm_start:right_arm_end].tolist(),
        "右手": contour_points[right_hand_start:right_hand_end].tolist(),
        "右腿": contour_points[right_leg_start:right_leg_end].tolist(),
        "右脚": contour_points[right_foot_start:right_foot_end].tolist(),
        "左脚": contour_points[left_foot_start:left_foot_end].tolist(),
        "左腿": contour_points[left_leg_start:left_leg_end].tolist(),
        "左手": contour_points[left_hand_start:left_hand_end].tolist(),
        "左臂": [contour_points[i].tolist() for i in left_arm_points]
    }
    
    return body_parts

def visualize_body_parts(image, body_parts, output_path):
    """
    可视化不同部位的轮廓点，连接各部位顶点形成轮廓
    
    参数:
        image: 原始图像
        body_parts: 按部位划分的轮廓点字典
        output_path: 输出图像路径
    """
    # 创建一个白色背景的空白图像
    height, width = image.shape[:2]
    img_parts = np.ones((height, width, 3), dtype=np.uint8) * 255
    
    # 定义不同部位的颜色
    colors = {
        "头部": (255, 0, 0),    # 红色
        "颈部": (255, 165, 0),  # 橙色
        "上身": (255, 255, 0),  # 黄色
        "右臂": (0, 255, 0),    # 绿色
        "右手": (0, 255, 255),  # 青色
        "右腿": (0, 0, 255),    # 蓝色
        "右脚": (128, 0, 128),  # 紫色
        "左脚": (255, 0, 255),  # 粉色
        "左腿": (139, 69, 19),  # 棕色
        "左手": (0, 128, 128),  # 青绿色
        "左臂": (128, 128, 0)   # 橄榄色
    }
    
    # 创建图像，绘制各部位的轮廓
    plt.figure(figsize=(15, 15))
    plt.imshow(img_parts)
    
    # 获取所有点并按顺序标号
    all_points = []
    global_index = 0
    
    # 创建一个字典，记录每个点的全局索引
    point_indices = {}
    
    # 首先收集所有点及其索引
    for part_name, points in body_parts.items():
        for point in points:
            point_tuple = tuple(point)  # 转换为元组以便用作字典键
            if point_tuple not in point_indices:
                point_indices[point_tuple] = global_index
                all_points.append((point, part_name, global_index))
                global_index += 1
    
    print(f"可视化的总点数: {len(all_points)}")
    
    # 为避免标签重叠，创建一个网格来跟踪已使用的位置
    grid_size = 20  # 网格单元大小
    used_grid_cells = set()
    
    # 绘制各部位的轮廓线
    for part_name, points in body_parts.items():
        if len(points) > 0:
            try:
                points_array = np.array(points)
                color_rgb = [x/255 for x in colors[part_name]]
                
                # 绘制连接线
                plt.plot(points_array[:, 0], points_array[:, 1], '-', color=color_rgb, linewidth=3, label=part_name)
                
                # 绘制点
                plt.scatter(points_array[:, 0], points_array[:, 1], color=color_rgb, s=30)
            except Exception as e:
                print(f"绘制 {part_name} 轮廓时出错: {e}")
    
    # 为所有点标注全局序号
    for point, part_name, idx in all_points:
        try:
            # 尝试找到一个未使用的网格单元来放置标签
            found_cell = False
            
            # 定义可能的偏移方向
            offsets = [(1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)]
            
            # 从点的位置开始，尝试不同的偏移
            base_x, base_y = point[0], point[1]
            
            for distance in range(1, 6):  # 尝试不同的距离
                for dx, dy in offsets:
                    # 计算标签位置
                    label_x = base_x + dx * distance * 10
                    label_y = base_y + dy * distance * 10
                    
                    # 计算网格单元
                    grid_x = int(label_x / grid_size)
                    grid_y = int(label_y / grid_size)
                    grid_key = (grid_x, grid_y)
                    
                    if grid_key not in used_grid_cells:
                        # 找到未使用的网格单元
                        plt.text(label_x, label_y, f"{idx}", fontsize=8, color='black',
                                 bbox=dict(facecolor='white', alpha=0.7, edgecolor='none', pad=1))
                        used_grid_cells.add(grid_key)
                        found_cell = True
                        break
                
                if found_cell:
                    break
            
            # 如果没有找到合适的位置，就放在原点附近
            if not found_cell:
                plt.text(base_x, base_y - 15, f"{idx}", fontsize=8, color='black',
                         bbox=dict(facecolor='white', alpha=0.7, edgecolor='none', pad=1))
            
        except Exception as e:
            print(f"标注点序号时出错: {e}")
    
    plt.legend(loc='upper right', bbox_to_anchor=(1.3, 1))
    plt.title("人体各部位轮廓及全局点序号", fontsize=16)
    plt.axis('off')
    plt.tight_layout()
    
    try:
        plt.savefig(output_path, dpi=300, bbox_inches='tight', pad_inches=0.1)
        print(f"成功保存图像到 {output_path}")
    except Exception as e:
        print(f"保存图像时出错: {e}")
    
    # 创建填充区域图像
    plt.figure(figsize=(15, 15))
    plt.imshow(img_parts)
    
    # 绘制所有部位的填充区域
    for part_name, points in body_parts.items():
        if len(points) > 0:
            try:
                points_array = np.array(points)
                color_rgb = [x/255 for x in colors[part_name]]
                
                # 绘制填充区域
                plt.fill(points_array[:, 0], points_array[:, 1], color=color_rgb, alpha=0.5, label=part_name)
                
                # 绘制轮廓线
                plt.plot(points_array[:, 0], points_array[:, 1], '-', color=color_rgb, linewidth=2)
            except Exception as e:
                print(f"绘制填充区域 {part_name} 时出错: {e}")
    
    plt.legend(loc='upper right', bbox_to_anchor=(1.3, 1))
    plt.title("人体各部位填充区域", fontsize=16)
    plt.axis('off')
    plt.tight_layout()
    
    # 保存填充区域图像
    filled_output_path = output_path.replace('.png', '_filled.png')
    try:
        plt.savefig(filled_output_path, dpi=300, bbox_inches='tight', pad_inches=0.1)
        print(f"成功保存填充区域图像到 {filled_output_path}")
    except Exception as e:
        print(f"保存填充区域图像时出错: {e}")
    
    plt.close('all')

if __name__ == "__main__":
    # 设置输入图片路径和输出目录
    img_path = '/Users/macpro/Downloads/BodyMap/body_red_2.png'
    output_dir = '/Users/macpro/Downloads/BodyMap/output'
    
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    print(f"开始处理图像: {img_path}")
    print(f"输出目录: {output_dir}")
    
    # 提取轮廓
    contour_points = extract_body_contour(img_path, output_dir)
    
    print(f"已提取 {len(contour_points)} 个轮廓点")
    
    # 检查输出文件是否存在
    base_name = os.path.splitext(os.path.basename(img_path))[0]
    body_parts_json = os.path.join(output_dir, f"{base_name}_body_parts.json")
    body_parts_img = os.path.join(output_dir, f"{base_name}_body_parts.png")
    
    if os.path.exists(body_parts_json):
        print(f"确认: 按部位划分的JSON文件已创建: {body_parts_json}")
        # 读取并显示部分内容
        with open(body_parts_json, 'r') as f:
            body_parts = json.load(f)
            print("JSON文件内容预览:")
            for part, points in body_parts.items():
                print(f"  {part}: {len(points)} 个点")
    else:
        print(f"错误: 按部位划分的JSON文件未创建: {body_parts_json}")
    
    if os.path.exists(body_parts_img):
        print(f"确认: 按部位可视化的图像已创建: {body_parts_img}")
    else:
        print(f"错误: 按部位可视化的图像未创建: {body_parts_img}")
    
    # 显示可视化结果
    plt.show()
import json

# 读取 JSON 文件
input_file = '/Users/macpro/Downloads/BodyMap/output/body_red_2_contour copy.json'
output_file = '/Users/macpro/Downloads/BodyMap/output/min_max_values.json'

with open(input_file, 'r') as file:
    coordinates = json.load(file)

# 初始化最小值和最大值
x_min = float('inf')
x_max = float('-inf')
y_min = float('inf')
y_max = float('-inf')

# 初始化最小值和最大值对应的坐标
x_min_coord = None
x_max_coord = None
y_min_coord = None
y_max_coord = None

# 遍历坐标，计算最小值和最大值及其对应的坐标
for x, y in coordinates:
    if x < x_min:
        x_min = x
        x_min_coord = (x, y)
    if x > x_max:
        x_max = x
        x_max_coord = (x, y)
    if y < y_min:
        y_min = y
        y_min_coord = (x, y)
    if y > y_max:
        y_max = y
        y_max_coord = (x, y)

# 计算跨度
x_span = x_max - x_min
y_span = y_max - y_min

# 结果保存为字典
result = {
    "x_min": {"value": x_min, "coordinate": x_min_coord},
    "x_max": {"value": x_max, "coordinate": x_max_coord},
    "y_min": {"value": y_min, "coordinate": y_min_coord},
    "y_max": {"value": y_max, "coordinate": y_max_coord},
    "x_span": x_span,
    "y_span": y_span
}

# 写入新的 JSON 文件
with open(output_file, 'w') as file:
    json.dump(result, file, indent=4)

print(f"Min and max values saved to {output_file}")

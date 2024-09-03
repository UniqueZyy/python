import itertools

numbers = [1, 2, 3]
combinations = list(itertools.product(numbers, repeat=len(numbers)))

total_combinations = len(combinations)
print("总的组合数量为:", total_combinations)


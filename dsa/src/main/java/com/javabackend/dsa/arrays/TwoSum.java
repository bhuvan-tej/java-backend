package com.javabackend.dsa.arrays;

import java.util.HashMap;
import java.util.Map;

/**
 *  PROBLEM: Two Sum
 *
 *  QUESTION
 *  "Given an array of integers and a target value, return the
 *   indices of the two numbers that add up to the target.
 *   Assume exactly one solution exists. Can you do better than O(n²)?"
 *
 *  EXAMPLE:
 *    Input : nums = [2, 7, 11, 15], target = 9
 *    Output: [0, 1]  → because nums[0] + nums[1] = 2 + 7 = 9
 *
 *  KEY INSIGHT (this is what interviewers want to hear):
 *
 *  Instead of checking every pair (slow), ask yourself:
 *  "For each number I see, what is its COMPLEMENT to reach the target?"
 *
 *    complement = target - currentNumber
 *
 *  If the complement was already seen before → we found our pair!
 *  We use a HashMap to remember "what we've seen and where."
 *
 *  VISUAL (HashMap approach):
 *
 *  nums = [2, 7, 11, 15],  target = 9
 *
 *  Step 1: index=0, num=2,  complement=9-2=7   → 7 not in map → store {2:0}
 *  Step 2: index=1, num=7,  complement=9-7=2   → 2 IS in map  → return [map.get(2), 1] = [0, 1] ✅
 *
 *  HashMap state over time:
 *  ┌────────────────────────────────────────────┐
 *  │  After step 1:  { 2 → index 0 }           │
 *  │  After step 2:  FOUND! complement 2 at 0  │
 *  └────────────────────────────────────────────┘
 *
 *  APPROACHES — TIME & SPACE TRADEOFFS
 *
 *  Approach 1 - Brute Force (nested loops):
 *    Time:  O(n²) — check every pair
 *    Space: O(1)  — no extra memory
 *    Verdict: Works but interviewer will ask "can you do better?"
 *
 *  Approach 2 - HashMap (one pass):
 *    Time:  O(n)  — single pass through array
 *    Space: O(n)  — extra HashMap
 *    Verdict: ✅ This is the expected answer
 *
 */
public class TwoSum {

    // ─────────────────────────────────────────────────────────
    //  APPROACH 1: Brute Force — O(n²) time, O(1) space
    //  Use this to explain your thinking BEFORE jumping to optimal
    // ─────────────────────────────────────────────────────────
    public int[] twoSumBruteForce(int[] nums, int target) {
        // Try every possible pair (i, j) where j > i
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                // Check if this pair sums to target
                if (nums[i] + nums[j] == target) {
                    return new int[]{i, j}; // Found the answer
                }
            }
        }
        // Problem guarantees a solution exists, but good practice to handle this
        throw new IllegalArgumentException("No two sum solution found");
    }

    // ─────────────────────────────────────────────────────────
    //  APPROACH 2: HashMap — O(n) time, O(n) space  ✅ OPTIMAL
    //  This is what you present as your final solution
    // ─────────────────────────────────────────────────────────
    public int[] twoSum(int[] nums, int target) {
        // Map stores: number → its index in the array
        // This lets us instantly check "have I seen the complement before?"
        Map<Integer, Integer> seenNumbers = new HashMap<>();

        for (int currentIndex = 0; currentIndex < nums.length; currentIndex++) {
            int currentNumber = nums[currentIndex];

            // What number do we NEED to pair with currentNumber to reach target?
            int complement = target - currentNumber;

            // Has that complement been seen earlier in the array?
            if (seenNumbers.containsKey(complement)) {
                // YES! We found the pair.
                // complement's index came before currentIndex (since we stored it earlier)
                return new int[]{seenNumbers.get(complement), currentIndex};
            }

            // NO — store current number with its index for future lookups
            seenNumbers.put(currentNumber, currentIndex);
        }

        throw new IllegalArgumentException("No two sum solution found");
    }

    // ─────────────────────────────────────────────────────────
    //  MAIN: Test your solution with examples
    // ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        TwoSum solution = new TwoSum();

        // Test 1: Basic case
        int[] result1 = solution.twoSum(new int[]{2, 7, 11, 15}, 9);
        System.out.println("Test 1: [" + result1[0] + ", " + result1[1] + "]"); // Expected: [0, 1]

        // Test 2: Numbers not at start
        int[] result2 = solution.twoSum(new int[]{3, 2, 4}, 6);
        System.out.println("Test 2: [" + result2[0] + ", " + result2[1] + "]"); // Expected: [1, 2]

        // Test 3: Duplicate numbers
        int[] result3 = solution.twoSum(new int[]{3, 3}, 6);
        System.out.println("Test 3: [" + result3[0] + ", " + result3[1] + "]"); // Expected: [0, 1]
    }

}
